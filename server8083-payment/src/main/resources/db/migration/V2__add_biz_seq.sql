-- V2__add_biz_seq.sql
-- 对外单号日序号表。支付库存 PAYMENT 与 REFUND；与物流库同构，各自管自己的单号类型。

CREATE TABLE biz_seq (
  biz         VARCHAR(16)  NOT NULL COMMENT '单号类型：PAYMENT支付单 REFUND退款单',
  seq_date    DATE         NOT NULL COMMENT '日期，按 Asia/Shanghai',
  current_seq INT UNSIGNED NOT NULL COMMENT '当日已用序号',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (biz, seq_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='对外单号日序号表';
