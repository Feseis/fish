-- V1__init_sms_schema.sql
-- 短信服务初始表结构（仅结构）
-- 依据：草稿/短信表草稿.md

CREATE TABLE sms_record (
  sms_record_id   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '发送记录主键，自增',
  request_id      CHAR(36)        NOT NULL COMMENT '调用方生成的幂等号，唯一',
  template_code   VARCHAR(32)     NOT NULL COMMENT '模板码，对应代码枚举，如 PAY_REMIND',
  target_phone    VARCHAR(20)     NOT NULL COMMENT '接收手机号',
  content         VARCHAR(600)    NOT NULL COMMENT '渲染后的完整文案（敏感内容已脱敏），发送时快照',
  source_service  TINYINT UNSIGNED NOT NULL COMMENT '调用方：1认证 2物流 3支付；服务码全库统一',
  biz_type        TINYINT UNSIGNED NULL COMMENT '业务类型，沿用全库统一词表；短信服务不解释',
  biz_id          CHAR(36)        NULL     COMMENT '调用方业务主键',
  result          TINYINT UNSIGNED NOT NULL COMMENT '1成功 2失败',
  fail_reason     VARCHAR(128)    NULL     COMMENT '失败原因，不含 apiCode',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间，即时发送无需另设 sent_at',

  PRIMARY KEY (sms_record_id),
  UNIQUE KEY uk_request (request_id),
  KEY idx_phone_time (target_phone, created_at),
  KEY idx_biz (biz_type, biz_id),
  KEY idx_template_time (template_code, created_at),
  KEY idx_source_time (source_service, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='短信发送记录表（append-only）';
