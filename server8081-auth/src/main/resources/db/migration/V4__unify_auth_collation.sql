-- V4__unify_auth_collation.sql
-- 将 auth 六张表统一为 utf8mb4_general_ci，必须先于 V5 执行。
-- 顺序：删外键 → 转换 → 重建外键。

ALTER TABLE auth_account_role DROP FOREIGN KEY fk_ar_account;
ALTER TABLE auth_account_role DROP FOREIGN KEY fk_ar_role;
ALTER TABLE auth_password     DROP FOREIGN KEY fk_pwd_account;
ALTER TABLE auth_session      DROP FOREIGN KEY fk_session_account;
ALTER TABLE auth_login_log    DROP FOREIGN KEY fk_log_account;

ALTER TABLE auth_account      CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE auth_role         CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE auth_account_role CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE auth_password     CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE auth_session      CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE auth_login_log    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

ALTER TABLE auth_account_role
  ADD CONSTRAINT fk_ar_account FOREIGN KEY (auth_account_id) REFERENCES auth_account (auth_account_id),
  ADD CONSTRAINT fk_ar_role    FOREIGN KEY (auth_role_id)    REFERENCES auth_role (auth_role_id);

ALTER TABLE auth_password
  ADD CONSTRAINT fk_pwd_account FOREIGN KEY (auth_account_id) REFERENCES auth_account (auth_account_id);

ALTER TABLE auth_session
  ADD CONSTRAINT fk_session_account FOREIGN KEY (auth_account_id) REFERENCES auth_account (auth_account_id);

ALTER TABLE auth_login_log
  ADD CONSTRAINT fk_log_account FOREIGN KEY (auth_account_id) REFERENCES auth_account (auth_account_id);
