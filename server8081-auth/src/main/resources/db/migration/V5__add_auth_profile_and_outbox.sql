-- V5__add_auth_profile_and_outbox.sql
-- 用户资料表、跨库同步事件表、角色改名、存量账号资料回填。

CREATE TABLE auth_user_profile (
  auth_user_profile_id CHAR(36)     NOT NULL COMMENT '资料主键 UUID；业务侧不引用此 ID',
  auth_account_id      CHAR(36)     NOT NULL COMMENT '→ auth_account.auth_account_id，1:1',
  real_name            VARCHAR(32)  NULL     COMMENT '真实姓名；申请成为员工前必须补齐',
  nickname             VARCHAR(32)  NULL     COMMENT '昵称，App 展示用',
  avatar_url           VARCHAR(255) NULL     COMMENT '头像地址；上传能力后置，当前只存 URL',
  gender               TINYINT UNSIGNED NULL COMMENT '1男 2女；NULL 表示未填写',
  birthday             DATE         NULL     COMMENT '生日',
  email                VARCHAR(64)  NULL     COMMENT '邮箱，只记录不验证',
  created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (auth_user_profile_id),
  UNIQUE KEY uk_auth_account (auth_account_id),
  CONSTRAINT fk_profile_account FOREIGN KEY (auth_account_id) REFERENCES auth_account (auth_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='用户基本信息表';

CREATE TABLE auth_outbox (
  auth_outbox_id  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '事件主键，自增',
  target_service  TINYINT UNSIGNED NOT NULL COMMENT '目标服务：2物流；服务码全库统一',
  event_type      TINYINT UNSIGNED NOT NULL COMMENT '本表独立事件词表：1姓名变更 2手机号变更；不与 event_outbox、payment_outbox 共用',
  auth_account_id CHAR(36)         NOT NULL COMMENT '主体账号 → auth_account.auth_account_id',
  old_value       VARCHAR(64)      NULL     COMMENT '变更前的值',
  new_value       VARCHAR(64)      NULL     COMMENT '变更后的值；空串表示清空',
  status          TINYINT          NOT NULL DEFAULT 1 COMMENT '1待投递 2已投递 3待重试 4已放弃',
  retry_count     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已重试次数',
  next_retry_at   DATETIME         NULL     COMMENT '下次重试时间；待投递为 NULL',
  last_error      VARCHAR(255)     NULL     COMMENT '最近一次失败原因',
  created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (auth_outbox_id),
  KEY idx_status_next (status, next_retry_at),
  KEY idx_account (auth_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='认证服务事件投递表（Outbox）';

UPDATE auth_role SET role_name = '物流员工' WHERE auth_role_id = 2;

INSERT INTO auth_user_profile (auth_user_profile_id, auth_account_id)
SELECT UUID(), a.auth_account_id
FROM auth_account a
WHERE NOT EXISTS (
  SELECT 1 FROM auth_user_profile p WHERE p.auth_account_id = a.auth_account_id
);
