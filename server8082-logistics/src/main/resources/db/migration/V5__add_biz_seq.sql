-- V5__add_biz_seq.sql
-- 对外单号日序号表。物流库存 WAYBILL、HANDOVER；支付库后续建同构表存 PAYMENT、REFUND。

CREATE TABLE biz_seq (
  biz         VARCHAR(16)  NOT NULL COMMENT '单号类型：WAYBILL运单 HANDOVER交接单',
  seq_date    DATE         NOT NULL COMMENT '日期，按 Asia/Shanghai',
  current_seq INT UNSIGNED NOT NULL COMMENT '当日已用序号',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (biz, seq_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='对外单号日序号表';
