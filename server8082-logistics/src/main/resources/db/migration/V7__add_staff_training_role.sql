-- V7__add_staff_training_role.sql
-- 培训凭证按岗位签发：新增 staff_role，去掉核销字段，同一账号同一岗位只保留一条凭证。

ALTER TABLE staff_training
  ADD COLUMN staff_role TINYINT UNSIGNED NOT NULL
    COMMENT '凭证对应岗位：1快递员 2客服 3司机；入职申请的 apply_staff_role 必须与之一致'
    AFTER staff_training_id;

ALTER TABLE staff_training
  MODIFY COLUMN trainee_name VARCHAR(32) NOT NULL
    COMMENT '姓名快照，发证时取自 auth_user_profile.real_name；由 auth_outbox 姓名变更事件同步',
  MODIFY COLUMN trainee_phone VARCHAR(20) NULL
    COMMENT '手机号快照，发证时取自 auth_account.phone',
  DROP COLUMN voucher_used_at;

ALTER TABLE staff_training
  ADD UNIQUE KEY uk_account_role (trainee_auth_account_id, staff_role);
