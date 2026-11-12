-- V3__add_channel_pay_url.sql
-- 渠道支付链接：支付宝沙箱 precreate 返回的 qr_code；模拟支付为空。

ALTER TABLE payment
  ADD COLUMN channel_pay_url VARCHAR(512) NULL
      COMMENT '渠道支付链接或二维码内容；模拟支付与已支付单为空';
