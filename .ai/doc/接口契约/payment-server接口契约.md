# 支付服务接口契约

用途：物流服务与客户端对接依据。改接口前读；与其他文档冲突时以本文为准。
更新日期：2026-11-12

## 1 服务信息

| 项 | 值 |
| --- | --- |
| 模块目录 | `server8083-payment` |
| 服务身份 | `payment-server` |
| 端口 | 8083 |
| 直连调试地址 | `http://localhost:8083` |
| 鉴权 | 当前无鉴权，属内部接口；内部令牌另行一轮接入 |

## 2 接口列表

| 接口 | 方法 | 状态 | 说明 |
| --- | --- | --- | --- |
| `/payment/internal/orders` | POST | 已实现 | 内部接口：建支付单，按 `biz_type + biz_id` 幂等；仅物流调用 |
| `/payment/internal/biz-terminated` | POST | 已实现 | 内部接口：业务终止（取消/退回），关待支付单、已支付单建全额退款 |
| `/payment/orders/{id}` | GET | 已实现 | 支付单详情 |
| `/payment/orders?bizType=&bizId=` | GET | 已实现 | 按业务查未终结支付单 |
| `/payment/orders/{id}/mock-pay` | POST | 已实现 | 模拟支付成功，仅 `pay_channel=1` |
| `/payment/orders/{id}/sync` | POST | 已实现 | 支付宝主动查单，仅 `pay_channel=2` |
| `/payment/alipay/notify` | POST | 已实现 | 支付宝异步通知，验签后返回纯文本 `success` |
| `/payment/orders/{id}/close` | POST | 已实现 | 关闭未支付单 |

## 3 建单

`POST /payment/internal/orders`

```json
{
  "bizType": 1,
  "bizId": "<waybillId>",
  "amount": 14.00,
  "payChannel": 1,
  "payMethod": 1,
  "payerAccountId": "<authAccountId>",
  "payerPhone": "13606666666"
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `bizType` | 是 | 全库统一词表：`1运单 5退回运费` |
| `bizId` | 是 | `bizType=1` 为 `waybill_id`，`bizType=5` 为 `waybill_return_id` |
| `amount` | 是 | > 0，2 位小数 |
| `payChannel` | 是 | `1模拟支付 2支付宝沙箱`；2 需 `ALIPAY_ENABLED=true` 且密钥齐全，否则 422 |
| `payMethod` | 是 | `1寄付`（30 分钟有效）`2到付`（48 小时有效） |
| `payerAccountId` | 否 | 空表示匿名付款，返回 `payToken` |
| `payerPhone` | 否 | 付款人手机号快照 |

幂等：同 `biz_type + biz_id` 下已存在 `status IN (1,2,6)` 的单，直接返回已有单，不新建。

成功返回 201：

```json
{"paymentId":"...","paymentNo":"P26111000000001","bizType":1,"bizId":"...",
 "amount":14.00,"payChannel":1,"status":1,"payToken":null,"qrCode":null,"payRemark":null,
 "expireAt":"2026-11-10T16:30:00","paidAt":null,"closedAt":null,
 "createdAt":"2026-11-10T16:00:00"}
```

`paymentNo` 规则 `P + yyMMdd + 8 位日序号`，由 `biz_seq` 生成，纯字母数字，可直接作渠道 `out_trade_no`。

`payChannel=2` 时建单会同步调 `alipay.trade.precreate`，`qrCode` 返回二维码内容并落 `channel_pay_url`；precreate 失败整笔回滚，返回 502 `CHANNEL_ERROR`。

错误码：`INVALID_PARAM` 400、`PAYMENT_AMOUNT_INVALID` 422、`CHANNEL_NOT_SUPPORTED` 422、`CHANNEL_ERROR` 502。

## 4 模拟支付

`POST /payment/orders/{id}/mock-pay`

条件更新 `WHERE pay_channel=1 AND status IN (1,3,6)`，影响行数为 1 才算成功。成功后写 `paid_at` 与 `paid_biz_key = biz_type:biz_id`。

同业务已有别的成功单时，本单记为重复支付：状态仍置 2，但 `paid_biz_key` 留空、`pay_remark='重复支付，待人工退款'`。

错误码：`PAYMENT_NOT_FOUND` 404、`PAYMENT_STATUS_INVALID` 409、`CHANNEL_NOT_SUPPORTED` 422。

## 5 关闭

`POST /payment/orders/{id}/close`：`status IN (1,3) → 4`，写 `closed_at`、清 `pay_token` 与 `next_remind_at`。否则 409。

## 6 支付宝沙箱

`POST /payment/orders/{id}/sync`：调 `alipay.trade.query`。`TRADE_SUCCESS` / `TRADE_FINISHED` 入账；`WAIT_BUYER_PAY`、`ACQ.TRADE_NOT_EXIST` 属正常中间态，原样返回当前状态。

`POST /payment/alipay/notify`：公开接口，不要求登录，安全性完全依赖验签。

- 验签失败返回纯文本 `failure`
- 金额与本地不一致返回 `failure`
- 非成功状态返回 `success`（避免支付宝重试）
- 入账成功或重复通知都返回 `success`

入账三种结果：

| 情况 | 处理 |
| --- | --- |
| 正常 | `status=2`、写 `channel_trade_no`、`paid_biz_key=biz_type:biz_id` |
| 重复通知（同一 `channel_trade_no`） | 幂等返回，不重复入账 |
| 关闭后迟到支付 | `status=2`、写渠道流水、`pay_remark='关闭后迟到支付'`，不占 `paid_biz_key` |

模拟渠道的渠道字段统一写 `MOCK-T-{paymentNo}`，与沙箱保持同一数据形态；退款时同样写 `MOCK-R-{refundNo}`。

## 7 物流侧调用约定

寄付运单（`payMethod=1`）由 logistics 在下单事务内同步调 `POST /payment/internal/orders`，`payChannel` 取订单入参，未传默认 1。调用失败整笔下单回滚，返回 502 `PAYMENT_UNAVAILABLE`。

到付运单（`payMethod=2`）下单时不建支付单，等运单进入 `4派送中` 再建，属 5-3 范围。

## 8 业务终止结算

`POST /payment/internal/biz-terminated`，由 logistics 投递 `event_outbox(event_type=3)` 时调用。

```json
{"waybillId":"...","bizType":2,"bizId":"<waybillCancelId>","eventOutboxId":12}
```

按 `waybillId` 取该运单下全部支付单，逐条分流：

| 支付单状态 | 处理 |
| --- | --- |
| 1 待支付 / 3 支付失败 | 关闭 `→ 4`，写 `closed_at` |
| 2 已支付 | 建 `refund(status=1)`；模拟渠道立即置 `2` 已退款并写 `payment_outbox(event_type=2)` |
| 4 已关闭 / 5 已退款 | 跳过 |

`refund_source`：`bizType=2`（运单取消）→ `1`；`bizType=1`（运单退回）→ `4`。退款单号 `R + yyMMdd + 8 位日序号`。

幂等：`refund.uk_payment` 唯一，事件重复投递不会再建退款单。

```json
{"closedCount":1,"refundCreatedCount":1,"skippedCount":0}
```

支付宝渠道本轮只建 `status=1` 待退款单，不调渠道退款接口；模拟渠道写 `channel_refund_no=MOCK-R-{refundNo}`。
