-- V8__add_staff_seq.sql
-- 员工工号全局序列：KD-00000001，8 位流水，不按日重置。

CREATE TABLE staff_seq (
  seq_name    VARCHAR(16)  NOT NULL COMMENT '序列名，当前只有 STAFF',
  current_seq INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已发序号',
  updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (seq_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='员工工号全局序列';

INSERT INTO staff_seq (seq_name, current_seq) VALUES ('STAFF', 0);
