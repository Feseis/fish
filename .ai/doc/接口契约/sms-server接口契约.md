# 短信服务接口契约

用途：各服务发送短信的唯一依据。改接口前读；与其他文档冲突时以本文为准。
更新日期：2026-11-12

## 1 服务信息

| 项 | 值 |
| --- | --- |
| 模块目录 | `server8084-sms` |
| 服务身份 | `sms-server` |
| 端口 | 8084 |
| 直连调试地址 | `http://localhost:8084` |
| 鉴权 | 当前无鉴权，属内部接口；内部令牌另行一轮接入 |
| 下游渠道 | fish-pbox `POST /sms/send-api` |

## 2 接口列表

| 接口 | 方法 | 状态 | 说明 |
| --- | --- | --- | --- |
| `/sms/internal/send` | POST | 已实现 | 内部接口：渲染模板并发送，按 `requestId` 幂等 |
| `/sms/internal/phones/exists` | GET | 已实现 | 内部接口：查询手机号是否在沙盒登记，返回 `registered` |

## 3 发送

`POST /sms/internal/send`

```json
{
  "requestId": "4f1c2f8e-8f0a-4c2f-9a1e-0d6b7c3a5e21",
  "templateCode": "LOGIN_CODE",
  "targetPhone": "13606666666",
  "sourceService": 1,
  "bizType": null,
  "bizId": null,
  "params": ["123456"]
}
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `requestId` | String(36) | 是 | 调用方生成的幂等号，建议 UUID |
| `templateCode` | String(32) | 是 | 模板码，见第 4 节 |
| `targetPhone` | String(20) | 是 | 接收手机号 |
| `sourceService` | Integer | 是 | 调用方服务码：1认证 2物流 3支付 4短信 |
| `bizType` | Integer | 否 | 全库统一业务类型词表，短信服务不解释 |
| `bizId` | String(36) | 否 | 调用方业务主键 |
| `params` | String[] | 是 | 模板参数，按 `{1}`..`{n}` 顺序对应 |

响应（成功与失败均 HTTP 200）：

```json
{
  "smsRecordId": 12,
  "requestId": "4f1c2f8e-8f0a-4c2f-9a1e-0d6b7c3a5e21",
  "result": 1,
  "failReason": null
}
```

| 字段 | 说明 |
| --- | --- |
| `result` | 1成功 2失败 |
| `failReason` | 失败原因，不含 `apiCode` |

口径：

- 同一 `requestId` 重复请求直接返回首次结果（含失败），不重复下发。
- 失败后调用方要重试必须换新 `requestId`。
- 模板参数个数与模板声明不一致返回 400 `INVALID_PARAM`；未知模板码返回 400 `TEMPLATE_NOT_FOUND`。
- 每次发送都写 `sms_record`，append-only；`content` 为脱敏后全文。

## 4 模板

签名前缀 `【飞鱼速递】` 由代码统一拼接。

| 模板码 | messageType | 参数个数 | 正文 | 落库脱敏 |
| --- | --- | --- | --- | --- |
| `LOGIN_CODE` | CODE | 1 | 验证码提醒：尊敬的用户，您正在使用验证码登录，验证码为 {1} | 参数1整段替换为 `******` |
| `PAY_REMIND` | NOTICE | 4 | 您的到付快件 {1} 应付 {2} 元，请在 {3} 前完成支付：{4} | 参数4按 URL 脱敏 |
| `PAY_REMIND_FINAL` | NOTICE | 2 | 您的到付快件 {1} 应付 {2} 元，已逾期，请联系快递员处理 | 无 |
| `ARREARS_REMIND` | NOTICE | 3 | 您的快件 {1} 退回运费 {2} 元尚未支付，请及时结清：{3} | 参数3按 URL 脱敏 |
| `ARRIVAL_NOTICE` | NOTICE | 2 | 您的快件 {1} 已到达 {2}，请及时领取 | 无 |

URL 脱敏规则：只保留 `scheme://host[:port]` 与路径末段短码，中间路径以 `…` 代替；无法解析时整段替换为 `******`。

## 5 错误

统一 RFC 9457 `ProblemDetail`，`Content-Type: application/problem+json`，自定义属性挂 `code`、`timestamp`。

| code | HTTP | 场景 |
| --- | --- | --- |
| `INVALID_PARAM` | 400 | 参数校验失败、参数个数不匹配 |
| `TEMPLATE_NOT_FOUND` | 400 | 未知模板码 |
| `NOT_FOUND` | 404 | 路径不存在 |
| `INTERNAL` | 500 | 未预期异常 |

## 6 调用约定

- 每个业务触发生成新的 `requestId`，不要复用；复用即视为重放。
- 发送失败不抛 5xx，调用方必须检查 `result`，为 2 时按自身策略处理。
- pbox 凭证只在短信服务配置（`sms.env` 的 `PBOX_*`），调用方不持有。
