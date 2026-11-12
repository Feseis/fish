-- V6__handover_pending_and_waybill_delivery.sql
-- 1) 交接单支持「待发」：发车时间可空，状态加 0待发，便于分批扫码装车。
-- 2) 运单增加当前网点快照，装车校验不再反推历史交接记录。
-- 3) 运单增加签收信息，支持他人代收登记姓名与手机号。

ALTER TABLE handover
  MODIFY COLUMN departed_at DATETIME NULL COMMENT '发车时间；待发为 NULL',
  MODIFY COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '0待发 1在途 2已到达';

ALTER TABLE waybill
  ADD COLUMN current_site_id CHAR(36) NULL
      COMMENT '当前所在网点 → logistics_site.logistics_site_id；揽收时写寄件网点，到达时更新',
  ADD COLUMN sign_type TINYINT UNSIGNED NULL
      COMMENT '1本人签收 2他人代收 3驿站签收；未签收为 NULL',
  ADD COLUMN sign_name VARCHAR(32) NULL
      COMMENT '签收人姓名；本人签收可空，他人代收必填',
  ADD COLUMN sign_phone VARCHAR(20) NULL
      COMMENT '签收人手机号；本人签收可空，他人代收必填',
  ADD KEY idx_current_site (current_site_id);
