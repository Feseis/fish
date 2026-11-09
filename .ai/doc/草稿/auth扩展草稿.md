# auth 扩展草稿

状态：草稿（不长期维护）
用途：认证服务补充用户资料、跨库同步事件、角色名与内部接口的设计稿；定稿后分流至决策日志与待办。
更新时间：2026-11-09

## 已裁决口径

### 用户资料

- `auth_user_profile` 与 `auth_account` 1:1，`auth_account_id` 唯一；业务库只引用 `auth_account_id`，**不得引用 `auth_user_profile_id`**。
- 资料行在**注册时同事务插入**，字段全部为 NULL；不在首次填写时懒创建，避免并发 upsert 与「资料不存在」分支。
- `real_name` 是姓名权威；业务表当前态（`logistics_staff.staff_name`、`logistics_site.leader_name`）是它的副本，历史凭证（`waybill_trace.operator_name` 等）永不更新。
- 申请成为员工前必须 `real_name` 非空，由物流服务调内部资料接口校验，不信任客户端提交的姓名。
- `gender` 只定义真实枚举，未知交给 NULL，不用 0 表示未知。
- `email` 只记录，不验证，不要求唯一。
- `avatar_url` 只存 URL，头像上传能力后置，当前占位。
- **实名认证不做**：`verified` 字段删除，不再占位；整项进搁置 G-01。`real_name` 是用户自填值，不代表已核验身份。

### 角色

- 员工权限角色使用现有 `auth_role_id=2`，不新增 `4 员工`。
- `auth_role.role_name` 由「物流人员」改为「物流员工」，`role_id=2` 不变；所有引用只认码值，不认名称。
- 权限角色只回答「能不能进员工端、能不能执行物流作业」；岗位差异由 `logistics_staff.staff_role` 表达，两者不混用。
- 快递员、客服、司机统一持有 `2 物流员工`；驿站管理员是 `3`，保持独立。
- 授予/撤销接口幂等：授予时角色已存在返回 200，撤销时角色不存在返回 200，Outbox 重试永不失败。

### 跨库同步

- auth 侧新增 `auth_outbox`，用于把资料变更推送给业务库，避免业务快照永久滞后。
- 变更事件少，直接投递具体值，不做「发事件 + 业务回查」两段式。
- 事件类型：`1姓名变更`、`2手机号变更`；值用 `old_value`/`new_value` 通用列，与其他 Outbox 保持同一结构风格。
- 消费方按 `auth_account_id` 直接 UPDATE，重复投递无副作用，不需要消费位点表。
- 姓名变更需要同步两处：`logistics_staff.staff_name`，以及按 `logistics_staff_id` 同步 `logistics_site.leader_name`。
- 手机号变更同步 `logistics_staff.staff_phone`。

### 接口

- 用户侧：`GET /auth/profile`、`PUT /auth/profile`。
- 服务侧：`GET /auth/internal/profile/{authAccountId}`、`POST /auth/internal/roles/grant`、`DELETE /auth/internal/roles/revoke`。
- 内部接口鉴权机制未定，先保留 `/auth/internal/**` 前缀，鉴权放待办。
- 内部资料接口同时返回 `auth_account.status`，供物流服务在入职核验时判断账号是否可用。
- 手机号变更接口 `PUT /auth/phone` 属于规划中：改手机号需要旧凭证验证 + 新手机号验证码；未实现前 `auth_outbox.event_type=2` 不会被触发。

## auth_user_profile

```sql
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
```

只允许更新 `real_name`、`nickname`、`avatar_url`、`gender`、`birthday`、`email`；手机号在 `auth_account`，走独立接口。

## auth_outbox

```sql
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
```

## 接口清单

| 接口 | 方法 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| `/auth/profile` | GET | 用户令牌 | 读自己的资料 |
| `/auth/profile` | PUT | 用户令牌 | 全量更新自己的资料 |
| `/auth/internal/profile/{authAccountId}` | GET | 待定（待办） | 返回资料与账号状态，供物流服务入职核验与姓名快照 |
| `/auth/internal/roles/grant` | POST | 待定（待办） | 授予角色，已存在返回 200 |
| `/auth/internal/roles/revoke` | DELETE | 待定（待办） | 撤销角色，不存在返回 200 |
| `/auth/phone` | PUT | 规划中 | 改手机号：旧凭证验证 + 新手机号验证码 |

## 迁移计划

**V4：collation 统一（必须先做）**

1. 删除 5 个外键：`fk_ar_account`、`fk_ar_role`、`fk_pwd_account`、`fk_session_account`、`fk_log_account`。
2. 六张表逐张 `CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci`。
3. 重建 5 个外键（同 V3 的约束名）。

**V5：结构**

1. 建 `auth_user_profile`（含 `fk_profile_account`）；此时两端已是 `general_ci`，外键可建。
2. 建 `auth_outbox`。
3. `UPDATE auth_role SET role_name='物流员工' WHERE auth_role_id=2;`
4. 回填存量账号资料行：

```sql
INSERT INTO auth_user_profile (auth_user_profile_id, auth_account_id)
SELECT UUID(), a.auth_account_id
FROM auth_account a
WHERE NOT EXISTS (
  SELECT 1 FROM auth_user_profile p WHERE p.auth_account_id = a.auth_account_id
);
```

5. 改 `RegisterService`：注册事务内第四条 INSERT，插入空资料行。

顺序不能颠倒：V1 建表时未显式写 `COLLATE`，`auth_account.auth_account_id` 默认是 `utf8mb4_0900_ai_ci`；若先建 `general_ci` 的外键，MySQL 8 报 3780。

## 未决事项

统一见 `待办/auth-server待办.md` 的「设计未决」小节。本文件只保留已裁决口径与表结构。
