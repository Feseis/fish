# auth 接口契约

状态：有效（产出物，唯一对接标准）
用途：前后端对接依据。改接口前读；与其他文档冲突时以本文为准。
更新时间：2026-11-02

## 1 身份与调用方式

| 项 | 内容 |
| --- | --- |
| 模块身份 | `auth-server8081` |
| 服务身份 | `auth-server` |
| 端口 | 8081 |
| 直连调试地址 | `http://localhost:8081` |
| 网关 | 暂无，客户端直连 |

## 2 接口列表

| 接口 | 方法 | 状态 | 说明 |
| --- | --- | --- | --- |
| `/auth/health` | GET | 已实现 | 数据库与 Redis 组件状态；全 UP 返回 200，任一 DOWN 返回 503 |
| `/auth/login` | POST | 规划中 | 手机号 + 密码登录，签发短期令牌与 refresh |
| `/auth/refresh` | POST | 规划中 | 用 refresh 换新短期令牌 |
| `/auth/logout` | POST | 规划中 | 结束当前会话 |

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

`PWD_WRONG`、`NO_ACCOUNT`、`FROZEN`、`TOKEN_EXPIRED`、`INVALID_PARAM`、`RATE_LIMITED`、`NOT_FOUND`、`INTERNAL`。
