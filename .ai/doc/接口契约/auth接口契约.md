# auth 接口契约

状态：有效（产出物，唯一对接标准）
用途：前后端对接依据。改接口前读；与其他文档冲突时以本文为准。
更新时间：2026-11-12

## 1 身份与调用方式

| 项 | 内容 |
| --- | --- |
| 模块目录 | `server8081-auth` |
| 服务身份 | `auth-server` |
| 端口 | 8081 |
| 直连调试地址 | `http://localhost:8081` |
| 令牌头 | `Authorization: Bearer <accessToken>`；常量定义在 `contract` 模块 |
| 内部接口 | `/auth/internal/**` 当前无鉴权，内部令牌另行一轮接入（见待办 `F-09`） |
| 网关 | 暂无，客户端直连 |

## 2 接口列表

| 接口 | 方法 | 状态 | 说明 |
| --- | --- | --- | --- |
| `/auth/health` | GET | 已实现 | 数据库与 Redis 组件状态；全 UP 返回 200，任一 DOWN 返回 503 |
| `/auth/get-sms-code` | POST | 已实现 | 入参手机号，先校验号码已在短信沙盒登记，再生成验证码并调 sms-server 发送；60 秒内重复调用返回 `RATE_LIMITED` |
| `/auth/register` | POST | 已实现 | 手机号 + 验证码 + 密码注册，成功即自动登录，返回 201 与登录出参 |
| `/auth/login/password` | POST | 已实现 | 手机号 + 密码登录，签发短期令牌与 refresh |
| `/auth/login/sms` | POST | 已实现 | 手机号 + 验证码登录，签发短期令牌与 refresh |
| `/auth/refresh` | POST | 已实现 | 读 refresh cookie，签发新短期令牌；refresh 不轮换 |
| `/auth/logout` | POST | 已实现 | 结束会话并吊销请求携带的短期令牌，清 cookie；幂等 |
| `/auth/internal/audit/waybill-cancel` | POST | 已实现（占位） | 运单取消审核，当前固定返回 `approved=true`，不校验入参 |
| `/auth/internal/audit/staff-apply` | POST | 已实现（占位） | 员工入职审核，当前固定返回 `approved=true`，不校验入参 |
| `/auth/internal/verify` | POST | 已实现 | 内部接口：校验短期令牌，返回 `sessionId`、`accountId`、`roles`；供其他服务调用 |
| `/auth/internal/accounts/by-phone` | GET | 已实现 | 内部接口：按手机号查账号，返回 `accountId`、`phone`、`status`、`realName` |
| `/auth/internal/accounts` | POST | 已实现 | 内部接口：建账号（演示初始化），不校验验证码，幂等 |
| `/auth/internal/roles/grant` | POST | 已实现 | 内部接口：授予角色，幂等；入职流程同步调用 |
| `/auth/verify` | POST | 已实现 | 对外接口：校验短期令牌，返回 `sessionId`、`accountId`、`roles`；供 App 使用 |

`/auth/internal/verify` 与 `/auth/verify` 入参相同，令牌放在 `Authorization: Bearer <accessToken>`，与 `/auth/logout` 一致；
两者分开是为了让内部响应与对外响应各自演进。令牌无效或已登出返回 401 `TOKEN_EXPIRED`。该校验只读 Redis 与角色表，
不再复核账号状态与密码版本，冻结/改密的收敛点在 `/auth/refresh`。

规划中接口在实现前不得被前端按已完成对接。

## 3 响应约定

成功：直接返回 DTO，HTTP 200。

```json
{"database": true, "redis": true}
```

失败：`Content-Type: application/problem+json`，HTTP 状态由错误码决定。

```json
{
  "title": "手机号或密码错误",
  "status": 401,
  "detail": "手机号或密码错误",
  "code": "PWD_WRONG",
  "timestamp": "2026-11-02T10:12:33+08:00"
}
```

字段说明：`code` 是前端判断依据；`detail` 给人看；`timestamp` 为服务端时间。

## 4 错误码

以 `me.feseis.fish.auth.exp.ErrorCode` 为准，不在文档内复制枚举，避免两处不一致。当前已定义：

`PWD_WRONG`、`SMS_WRONG`、`SMS_EXPIRED`、`NO_ACCOUNT`、`FROZEN`、`CLOSED`、`PHONE_EXISTS`、`TOKEN_EXPIRED`、`INVALID_PARAM`、`RATE_LIMITED`、`SMS_SEND_FAILED`、`NOT_FOUND`、`INTERNAL`。
