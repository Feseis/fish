-- V3__rename_auth_columns.sql
-- 主键统一为「表名 + _id」，跨表引用用全名（D-022）。
-- 顺序：删外键 → 改名 → 补主键/唯一键 → 重建外键。

-- 1. 删除外键（被引用列改名前必须先解除约束）
ALTER TABLE auth_account_role DROP FOREIGN KEY fk_ar_account;
ALTER TABLE auth_account_role DROP FOREIGN KEY fk_ar_role;
ALTER TABLE auth_password     DROP FOREIGN KEY fk_pwd_account;
ALTER TABLE auth_session      DROP FOREIGN KEY fk_session_account;
ALTER TABLE auth_login_log    DROP FOREIGN KEY fk_log_account;

-- 2. auth_account / auth_role 主键改名
ALTER TABLE auth_account
  CHANGE COLUMN account_id auth_account_id CHAR(36) NOT NULL COMMENT '账号主键，UUID';

ALTER TABLE auth_role
  CHANGE COLUMN role_id auth_role_id TINYINT UNSIGNED NOT NULL COMMENT '角色码，固定枚举，非自增';

-- 3. auth_account_role：复合主键两列改名
ALTER TABLE auth_account_role
  CHANGE COLUMN account_id auth_account_id CHAR(36) NOT NULL COMMENT '账号ID',
  CHANGE COLUMN role_id    auth_role_id    TINYINT UNSIGNED NOT NULL COMMENT '角色码';

-- 4. auth_password：新增独立主键，原账号列改名为外键列
ALTER TABLE auth_password
  CHANGE COLUMN account_id auth_account_id CHAR(36) NOT NULL COMMENT '→ auth_account.auth_account_id，1:1';

ALTER TABLE auth_password DROP PRIMARY KEY;

ALTER TABLE auth_password
  ADD COLUMN auth_password_id CHAR(36) NULL COMMENT '密码记录主键，UUID' FIRST;

UPDATE auth_password SET auth_password_id = UUID();

ALTER TABLE auth_password
  MODIFY COLUMN auth_password_id CHAR(36) NOT NULL COMMENT '密码记录主键，UUID',
  ADD PRIMARY KEY (auth_password_id),
  ADD UNIQUE KEY uk_auth_account (auth_account_id);

-- 5. auth_session 主键与账号列改名
ALTER TABLE auth_session
  CHANGE COLUMN session_id auth_session_id CHAR(36) NOT NULL COMMENT '会话ID，UUIDv4 随机生成，即不透明 token 值',
  CHANGE COLUMN account_id auth_account_id CHAR(36) NOT NULL COMMENT '账号ID';

-- 6. auth_login_log 主键、账号列与会话列改名
ALTER TABLE auth_login_log
  CHANGE COLUMN log_id     auth_login_log_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  CHANGE COLUMN account_id auth_account_id   CHAR(36) NULL COMMENT '账号存在时填入，不存在为 NULL',
  CHANGE COLUMN session_id auth_session_id   CHAR(36) NULL COMMENT '成功时关联的会话';

-- 7. 重建外键
ALTER TABLE auth_account_role
  ADD CONSTRAINT fk_ar_account FOREIGN KEY (auth_account_id) REFERENCES auth_account (auth_account_id),
  ADD CONSTRAINT fk_ar_role    FOREIGN KEY (auth_role_id)    REFERENCES auth_role (auth_role_id);

ALTER TABLE auth_password
  ADD CONSTRAINT fk_pwd_account FOREIGN KEY (auth_account_id) REFERENCES auth_account (auth_account_id);

ALTER TABLE auth_session
  ADD CONSTRAINT fk_session_account FOREIGN KEY (auth_account_id) REFERENCES auth_account (auth_account_id);

ALTER TABLE auth_login_log
  ADD CONSTRAINT fk_log_account FOREIGN KEY (auth_account_id) REFERENCES auth_account (auth_account_id);
