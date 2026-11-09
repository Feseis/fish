# logistics-server 接口契约

状态：有效（产出物，唯一对接标准）
用途：前后端对接依据。改接口前读；与其他文档冲突时以本文为准。
更新时间：2026-11-09

## 1 身份与调用方式

| 项 | 内容 |
| --- | --- |
| 模块目录 | `server8082-logistics` |
| 服务身份 | `logistics-server` |
| 端口 | 8082 |
| 直连调试地址 | `http://localhost:8082` |
| 网关 | 暂无，客户端直连 |
| 鉴权 | `/logistics/price/**` 公开；`/logistics/address/**` 需 `Authorization: Bearer <accessToken>`，由本服务转发到 auth `POST /auth/verify` 换取账号 |

## 2 接口列表

| 接口 | 方法 | 状态 | 说明 |
| --- | --- | --- | --- |
| `/logistics/price/quote` | POST | 已实现 | 运费与保价费试算，只试算不落库 |
| `/logistics/address/regeo` | GET | 已实现 | 逆地理编码，返回行政区与候选 POI，不落库 |
| `/logistics/address/user` | GET | 已实现 | 当前账号地址簿 |
| `/logistics/address/user` | POST | 已实现 | 新增地址；撞唯一键复活软删行 |
| `/logistics/address/user/{id}` | PUT | 已实现 | 按 ID 更新地址 |
| `/logistics/address/user/{id}` | DELETE | 已实现 | 软删除地址 |

## 3 运费试算

`POST /logistics/price/quote`

入参：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `senderAdcode` | String | 是 | 寄件地区划码，6 位数字 |
| `receiverAdcode` | String | 是 | 收件地区划码，6 位数字 |
| `itemWeight` | Number | 是 | 计费重量 kg，> 0，最多 3 位小数 |
| `declaredValue` | Number | 否 | 申报价值 元，>= 0，最多 2 位小数；缺省或 0 表示不保价 |

出参：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `freightRelationId` | Integer | `1同城 2省内 3跨省` |
| `priceRuleId` | Long | 本次使用的运费规则 |
| `insuranceRuleId` | Long | 本次使用的保价规则；未保价时为 `null` |
| `freightAmount` | Number | 运费，2 位小数 |
| `insuredFee` | Number | 保价费，2 位小数；未保价为 `0.00` |
| `totalAmount` | Number | 应付总额，2 位小数 |

错误码：

| code | HTTP | 场景 |
| --- | --- | --- |
| `INVALID_PARAM` | 400 | 参数校验失败或请求体格式错误 |
| `PRICE_RULE_MISSING` | 422 | 该寄收关系没有启用的运费规则 |
| `INSURANCE_RULE_MISSING` | 500 | 申报保价但没有启用的保价规则 |
| `INSURANCE_LIMIT_EXCEEDED` | 422 | 申报价值超过保价规则上限 |
| `INTERNAL` | 500 | 未预期异常 |

示例：

```json
// 请求
{"senderAdcode":"330106","receiverAdcode":"330108","itemWeight":2.3,"declaredValue":1000}

// 响应
{"freightRelationId":1,"priceRuleId":1,"insuranceRuleId":1,
 "freightAmount":14.00,"insuredFee":5.00,"totalAmount":19.00}
```

失败响应为 RFC 9457 `ProblemDetail`，额外带 `code` 与 `timestamp`。

## 4 逆地理编码

`GET /logistics/address/regeo?lng={lng}&lat={lat}`，需 `Authorization: Bearer`。调用高德 `v3/geocode/regeo`（`extensions=all`、半径 1000m、按距离排序），只返回不落库。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `formattedAddress` | String | 高德格式化地址 |
| `province` / `city` / `district` | String | 行政区；直辖市 `city` 用 `province` 填充 |
| `adcode` | String | 6 位行政区划码 |
| `pois` | Array | 候选锚点，最多约 30 条，按距离升序 |

`pois[]`：

| 字段 | 说明 |
| --- | --- |
| `poiId` | 高德 POI ID，作 `geo_address` 缓存键 |
| `poiName` | POI 名称 |
| `address` | POI 地址描述 |
| `lng` / `lat` | POI 自身坐标，GCJ-02 |
| `adcode` / `province` / `city` / `district` | 取自定位点的 `addressComponent`，高德 POI 本身不返回行政区 |

坐标超出中国范围（经度 73–136、纬度 3–54）返回 400 `INVALID_PARAM`；高德不可用返回 502 `AMAP_UNAVAILABLE`。

## 5 地址簿

`GET /logistics/address/user` → 当前账号未删除的地址列表，按 `created_at` 倒序。

`POST /logistics/address/user` 入参：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `poiId` | 是 | 高德 POI ID |
| `poiName` | 否 | POI 名称 |
| `adcode` | 是 | 6 位行政区划码 |
| `province` / `city` / `district` | 否 | 行政区名称；`city` 为空时用 `province` 填充 |
| `lng` / `lat` | 是 | POI 坐标，GCJ-02 |
| `contactName` | 是 | 联系人，≤32 |
| `contactPhone` | 是 | 手机号 |
| `detailAddr` | 是 | 详细门牌，≤200 |
| `tag` | 否 | 家/公司/学校，≤16 |

返回 201 与地址条目。同一账号下 `(poiId, detailAddr, contactName, contactPhone)` 完全相同的未删除条目返回 409 `ADDRESS_DUPLICATED`；命中已软删条目则复活并返回原 `userAddressId`。

`PUT /logistics/address/user/{id}`：入参同新增，按 ID 更新；不属于当前账号或已删除返回 404 `ADDRESS_NOT_FOUND`。

`DELETE /logistics/address/user/{id}`：软删除，置 `deleted_at`；重复删除返回 404。

错误码：`UNAUTHORIZED` 401、`ADDRESS_NOT_FOUND` 404、`ADDRESS_DUPLICATED` 409、`AMAP_UNAVAILABLE` 502、`AUTH_UNAVAILABLE` 502。

