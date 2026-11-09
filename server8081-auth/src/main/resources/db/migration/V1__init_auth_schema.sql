-- V1__init_auth_schema.sql
-- 认证服务初始表结构（仅结构，不含种子数据）

-- 1. 账号表
CREATE TABLE auth_account (
  account_id  CHAR(36)    NOT NULL COMMENT '账号主键，UUID',
  phone       VARCHAR(20) NOT NULL COMMENT '手机号，11位纯数字，唯一登录标识',
  status      TINYINT     NOT NULL DEFAULT 1 COMMENT '1正常 2冻结 3注销',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (account_id),
  UNIQUE KEY uk_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号表';

-- 2. 角色字典表（0 保留给未分配/系统，1 起为实际角色）
CREATE TABLE auth_role (
  role_id     TINYINT UNSIGNED NOT NULL COMMENT '角色码，固定枚举，非自增',
  role_name   VARCHAR(32)      NOT NULL COMMENT '角色中文名',
  description VARCHAR(128)     NULL COMMENT '角色职责说明',
  PRIMARY KEY (role_id),
  UNIQUE KEY uk_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色字典表';

-- 3. 账号-角色关联表（多对多，永久授予）
CREATE TABLE auth_account_role (
  account_id CHAR(36)         NOT NULL COMMENT '账号ID',
  role_id    TINYINT UNSIGNED NOT NULL COMMENT '角色码',
  granted_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授予时间',
  PRIMARY KEY (account_id, role_id),
  KEY idx_role (role_id),
  CONSTRAINT fk_ar_account FOREIGN KEY (account_id) REFERENCES auth_account (account_id),
  CONSTRAINT fk_ar_role    FOREIGN KEY (role_id)    REFERENCES auth_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号角色关联表';

-- 4. 密码表（与账号 1:1，无记录代表未设置密码）
CREATE TABLE auth_password (
  account_id    CHAR(36)     NOT NULL COMMENT '账号ID',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希，自带盐',
  hash_algo     VARCHAR(16)  NOT NULL DEFAULT 'bcrypt' COMMENT 'bcrypt / argon2id',
  pwd_version   INT          NOT NULL DEFAULT 0 COMMENT '改密递增，用于会话失效',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (account_id),
  CONSTRAINT fk_pwd_account FOREIGN KEY (account_id) REFERENCES auth_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='密码表';

-- 5. 会话表（一次登录一行）
CREATE TABLE auth_session (
  session_id     CHAR(36) NOT NULL COMMENT '会话ID，UUIDv4 随机生成，即不透明 token 值',
  account_id     CHAR(36) NOT NULL COMMENT '账号ID',
  refresh_hash   CHAR(64) NOT NULL COMMENT 'SHA-256(refresh_token)，轮换时更新',
  pwd_version    INT      NOT NULL COMMENT '登录时密码版本快照',
  status         TINYINT  NOT NULL DEFAULT 1 COMMENT '1活跃 2登出 3被踢 4过期',
  login_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_active_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expire_at      DATETIME NOT NULL COMMENT 'refresh 绝对过期时间',
  logout_at      DATETIME NULL COMMENT '登出/踢下线时间',
  PRIMARY KEY (session_id),
  KEY idx_account_status (account_id, status),
  KEY idx_expire (expire_at),
  CONSTRAINT fk_session_account FOREIGN KEY (account_id) REFERENCES auth_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 6. 登录日志表（append-only）
CREATE TABLE auth_login_log (
  log_id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  account_id  CHAR(36)    NULL COMMENT '账号存在时填入，不存在为 NULL',
  phone       VARCHAR(20) NOT NULL COMMENT '本次尝试的手机号原值',
  login_type  VARCHAR(16) NOT NULL COMMENT 'PASSWORD / SMS',
  result      TINYINT     NOT NULL COMMENT '1成功 2失败',
  fail_reason VARCHAR(32) NULL COMMENT 'PWD_WRONG / NO_ACCOUNT / FROZEN / SMS_EXPIRED / SMS_WRONG / RATE_LIMITED',
  session_id  CHAR(36)    NULL COMMENT '成功时关联的会话',
  client_ip   VARCHAR(45) NULL COMMENT 'IPv6 兼容，服务端采集',
  created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (log_id),
  KEY idx_account_time (account_id, created_at),
  KEY idx_phone_time (phone, created_at),
  KEY idx_time (created_at),
  CONSTRAINT fk_log_account FOREIGN KEY (account_id) REFERENCES auth_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志表';
