-- 认证服务建库脚本
-- 用途：本地开发环境手动执行一次，创建 fish_auth 库。
-- 说明：建库不属于 Flyway 迁移（Flyway 连到已存在的库执行），因此单独放这里。

CREATE DATABASE IF NOT EXISTS fish_auth
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;
