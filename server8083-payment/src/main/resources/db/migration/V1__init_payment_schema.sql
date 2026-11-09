-- V1__init_payment_schema.sql
-- 支付服务初始表结构（仅结构，不含种子数据）
-- 依据：草稿/支付表草稿.md、决策日志 D-024

CREATE TABLE payment (
  payment_id       CHAR(36)      NOT NULL COMMENT '支付单主键 UUID',
  payment_no       VARCHAR(32)   NOT NULL COMMENT '对外支付单号：P+yyMMdd+8位日序号，纯字母数字，直接作渠道 out_trade_no',
  biz_type         TINYINT UNSIGNED NOT NULL COMMENT '业务类型（全库统一词表）：1运单 5退回运费',
  biz_id           CHAR(36)      NOT NULL COMMENT '业务主键：biz_type=1 时为 waybill.waybill_id；biz_type=5 时为 waybill_return.waybill_return_id',

  payer_type       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '付款人类型：1账号 2匿名（扫码链接）',
  payer_account_id CHAR(36)      NULL     COMMENT '付款账号 → auth_account.auth_account_id；payer_type=2 时为 NULL',
  payer_phone      VARCHAR(20)   NULL     COMMENT '付款人手机号快照；匿名时取运单收件人手机号',

  amount           DECIMAL(10,2) NOT NULL COMMENT '应付金额，单位元，取运单下单快照',
  pay_channel      TINYINT UNSIGNED NOT NULL COMMENT '1模拟支付 2支付宝沙箱 3微信',
  status           TINYINT       NOT NULL DEFAULT 1 COMMENT '1待支付 2已支付 3支付失败 4已关闭 5已全额退款 6已超时待介入；4→1 仅限 biz_type=5 欠费结清，6→1 仅限到付人工介入',
  channel_trade_no VARCHAR(64)   NULL     COMMENT '渠道交易号；模拟支付为空',
  paid_biz_key     VARCHAR(80)   NULL     COMMENT '成功占位：成功时填 biz_type:biz_id；唯一键保证同一业务只有一条成功支付；重复支付不填此列',
  pay_remark       VARCHAR(255)  NULL     COMMENT '失败原因或渠道返回信息；关闭后迟到支付记「关闭后迟到支付」',

  pay_token        VARCHAR(64)   NULL     COMMENT '匿名付款令牌，32 字节随机串；成功或过期后置 NULL',
  remind_count     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已发送催付提醒次数，上限 6；同时是定时任务的乐观锁版本号',
  next_remind_at   DATETIME      NULL     COMMENT '下次催付时间；已支付、已关闭或停止提醒为 NULL',

  expire_at        DATETIME      NOT NULL COMMENT '单次支付凭证有效期，也是 pay_token 有效期：寄付 30 分钟，到付匿名 48 小时；复活原单时按规则顺延',
  paid_at          DATETIME      NULL     COMMENT '支付成功时间',
  closed_at        DATETIME      NULL     COMMENT '关闭时间；欠费结清复活原单时清空',
  created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (payment_id),
  UNIQUE KEY uk_payment_no (payment_no),
  UNIQUE KEY uk_channel_trade (pay_channel, channel_trade_no),
  UNIQUE KEY uk_paid_biz (paid_biz_key),
  UNIQUE KEY uk_pay_token (pay_token),
  KEY idx_biz (biz_type, biz_id),
  KEY idx_status_expire (status, expire_at),
  KEY idx_remind (status, next_remind_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='支付单表';

CREATE TABLE refund (
  refund_id         CHAR(36)      NOT NULL COMMENT '退款单主键 UUID',
  refund_no         VARCHAR(32)   NOT NULL COMMENT '对外退款单号，唯一',
  payment_id        CHAR(36)      NOT NULL COMMENT '→ payment.payment_id；退款挂在支付单上',
  amount            DECIMAL(10,2) NOT NULL COMMENT '退款金额；当前策略恒等于支付金额',
  refund_source     TINYINT UNSIGNED NOT NULL COMMENT '触发来源：1运单取消 2异常处理 3人工 4运单退回；业务对象经 payment 反查',
  reason            VARCHAR(128)  NULL     COMMENT '退款原因，业务方传入文本',
  status            TINYINT       NOT NULL DEFAULT 1 COMMENT '1待退款 2已退款 3退款失败',
  channel_refund_no VARCHAR(64)   NULL     COMMENT '渠道退款流水号，唯一',
  refunded_at       DATETIME      NULL     COMMENT '退款成功时间',
  refund_remark     VARCHAR(255)  NULL     COMMENT '失败原因',
  created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (refund_id),
  UNIQUE KEY uk_refund_no (refund_no),
  UNIQUE KEY uk_payment (payment_id),
  UNIQUE KEY uk_channel_refund (channel_refund_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='退款单表';

CREATE TABLE payment_reminder (
  payment_reminder_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '提醒记录主键，自增',
  payment_id          CHAR(36)        NOT NULL COMMENT '→ payment.payment_id',
  remind_type         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1到付催付 2退回运费欠费催收',
  remind_no           TINYINT UNSIGNED NOT NULL COMMENT '第几次提醒：到付 1..6，欠费 1..3（第1/3/7天）',
  send_channel        TINYINT UNSIGNED NOT NULL COMMENT '1短信',
  target_phone        VARCHAR(20)     NOT NULL COMMENT '接收手机号，发送时快照',
  result              TINYINT UNSIGNED NOT NULL COMMENT '1成功 2失败',
  fail_reason         VARCHAR(128)    NULL     COMMENT '失败原因',
  created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间，即时发送无需另设 sent_at',

  PRIMARY KEY (payment_reminder_id),
  KEY idx_payment_remind (payment_id, remind_type, remind_no),
  KEY idx_payment_time (payment_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='支付催付提醒记录表（到付/欠费）';

CREATE TABLE payment_outbox (
  payment_outbox_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '事件主键，自增',
  target_service    TINYINT UNSIGNED NOT NULL COMMENT '目标服务：2物流；服务码全库统一',
  event_type        TINYINT UNSIGNED NOT NULL COMMENT '本表独立事件词表：1到付超时报备 2退款完成；不与 event_outbox 共用',
  biz_type          TINYINT UNSIGNED NOT NULL COMMENT '业务类型（全库统一词表）：1运单 5退回运费',
  biz_id            CHAR(36)        NOT NULL COMMENT '业务主键',
  payment_id        CHAR(36)        NULL     COMMENT '关联支付单',
  refund_id         CHAR(36)        NULL     COMMENT '关联退款单',
  status            TINYINT         NOT NULL DEFAULT 1 COMMENT '1待投递 2已投递 3待重试 4已放弃',
  retry_count       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已重试次数',
  next_retry_at     DATETIME        NULL     COMMENT '下次重试时间；待投递为 NULL',
  last_error        VARCHAR(255)    NULL     COMMENT '最近一次失败原因',
  created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (payment_outbox_id),
  KEY idx_status_next (status, next_retry_at),
  KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='支付服务事件投递表（Outbox）';
