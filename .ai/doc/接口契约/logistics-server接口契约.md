# logistics-server 接口契约

状态：有效（产出物，唯一对接标准）
用途：前后端对接依据。改接口前读；与其他文档冲突时以本文为准。
更新时间：2026-11-12

## 1 身份与调用方式

| 项 | 内容 |
| --- | --- |
| 模块目录 | `server8082-logistics` |
| 服务身份 | `logistics-server` |
| 端口 | 8082 |
| 直连调试地址 | `http://localhost:8082` |
| 网关 | 暂无，客户端直连 |
| 鉴权 | `/logistics/price/**` 公开；`/logistics/address/**`、`/logistics/waybill/**` 需 `Authorization: Bearer <accessToken>`，由本服务转发到 auth `POST /auth/internal/verify` 换取账号；`/logistics/internal/**` 当前不鉴权，内部令牌另行一轮接入 |

## 2 接口列表

| 接口 | 方法 | 状态 | 说明 |
| --- | --- | --- | --- |
| `/logistics/price/quote` | POST | 已实现 | 运费与保价费试算，只试算不落库 |
| `/logistics/address/regeo` | GET | 已实现 | 逆地理编码，返回行政区与候选 POI，不落库 |
| `/logistics/address/search` | GET | 已实现 | 关键字搜索 POI，可按城市限定，不落库 |
| `/logistics/address/user` | GET | 已实现 | 当前账号地址簿 |
| `/logistics/address/user` | POST | 已实现 | 新增地址；撞唯一键复活软删行 |
| `/logistics/address/user/{id}` | PUT | 已实现 | 按 ID 更新地址 |
| `/logistics/address/user/{id}` | DELETE | 已实现 | 软删除地址 |
| `/logistics/waybill` | POST | 已实现 | 下单，返回运单号与金额快照 |
| `/logistics/waybill/{id}` | GET | 已实现 | 运单详情 |
| `/logistics/waybill` | GET | 已实现 | 当前账号运单列表，`page`/`size` 分页 |
| `/logistics/waybill/{id}/cancel` | POST | 已实现 | 取消运单，用户与员工共用 |
| `/logistics/waybill/{id}/exception` | POST | 已实现 | 标记异常，仅在职员工 |
| `/logistics/waybill/{id}/exception/resolve` | POST | 已实现 | 解除异常，仅在职员工 |
| `/logistics/waybill/{id}/trace` | GET | 已实现 | 轨迹列表，归属人或在职员工可见 |
| `/logistics/waybill/{id}/pickup` | POST | 已实现 | 揽收，`1 → 2` |
| `/logistics/waybill/{id}/deliver` | POST | 已实现 | 签收，`4 → 5`，支持他人代收 |
| `/logistics/waybill/{id}/deliver/fail` | POST | 已实现 | 投递失败，状态不变 |
| `/logistics/handover` | POST | 已实现 | 创建交接单（待发） |
| `/logistics/handover/{id}/load` | POST | 已实现 | 装车，可分批 |
| `/logistics/handover/{id}/depart` | POST | 已实现 | 发车，`0 → 1` |
| `/logistics/handover/{id}/arrive` | POST | 已实现 | 到达，`1 → 2` |
| `/logistics/handover/{id}` | GET | 已实现 | 交接单详情与明细 |
| `/logistics/internal/outbox/dispatch` | POST | 已实现 | 内部接口：手动投递 `event_outbox` 到支付服务，`limit` 默认 50、上限 100 |
| `/logistics/internal/sites` | GET | 已实现 | 内部接口：全部网点列表，不过滤启用状态与类型 |
| `/logistics/internal/staff-trainings` | POST | 已实现 | 内部接口：签发培训凭证，按账号 + 岗位唯一 |
| `/logistics/staff/apply` | POST | 已实现 | 入职申请：用培训凭证换取员工身份，需登录 |

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

### 4.1 关键字搜索

`GET /logistics/address/search?keyword={keyword}&city={city}`，需 `Authorization: Bearer`。

调用高德 `v3/place/text`（`offset=20`、`page=1`、`extensions=base`）；`city` 非空时附加 `citylimit=true` 限定城市。返回 `pois[]` 结构与 §4 一致，`adcode` 与省市区取自 POI 自身。

`keyword` 为空或超过 32 字返回 400 `INVALID_PARAM`；高德不可用返回 502 `AMAP_UNAVAILABLE`。每次调用消耗 1 次高德 Web 服务配额，客户端只在用户点击搜索时请求。

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

## 6 下单

`POST /logistics/waybill`，需 `Authorization: Bearer`。

```json
{
  "sender":   {"name":"张三","phone":"13606666666","adcode":"330106","province":"浙江省",
               "city":"杭州市","district":"西湖区","detailAddr":"3 幢 501",
               "poiId":"B0FFFTRUN4","poiName":"黄龙国际中心","lng":120.127984,"lat":30.275405},
  "receiver": {"name":"李四","phone":"13900000000","adcode":"350203","province":"福建省",
               "city":"厦门市","district":"思明区","detailAddr":"2 号楼 801",
               "poiId":"B025003000","poiName":"厦门国贸大厦","lng":118.119205,"lat":24.475567},
  "itemName":"文件","itemCategoryId":1,"itemWeight":1.0,"declaredValue":1000,
  "payMethod":1,"remark":"易碎"
}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `sender` / `receiver` | 是 | 地址快照，字段同上；`adcode` 必须 6 位，`poiId` 必填 |
| `itemName` | 是 | ≤64 |
| `itemCategoryId` | 否 | 物品类别码 |
| `itemWeight` | 是 | kg，> 0，3 位小数 |
| `declaredValue` | 否 | 申报价值，> 0 才计保价费 |
| `payMethod` | 是 | `1寄付 2到付` |
| `payChannel` | 否 | `1模拟支付 2支付宝沙箱`，缺省 `1`；`payMethod=1` 建支付单时透传 |
| `remark` | 否 | ≤255 |

成功返回 201：

```json
{"waybillId":"...","waybillNo":"FF261109000001","status":1,"freightRelationId":3,
 "priceRuleId":3,"insuranceRuleId":1,"freightAmount":12.00,"insuredFee":5.00,
 "totalAmount":17.00,"createdAt":"2026-11-09T16:20:00"}
```

运单号规则 `FF + yyMMdd + 6 位日序号`，由数据库 `biz_seq` 表生成。

`payMethod=1`（寄付）时，本事务内同步调 payment `POST /payment/internal/orders` 建支付单；调用失败整笔回滚，返回 502 `PAYMENT_UNAVAILABLE`。`payMethod=2`（到付）下单不建单，等运单进入 `4派送中` 再建。

错误码：`DELIVERY_AREA_UNCOVERED` 422（寄件或收件 adcode 无营业网点覆盖）、`PRICE_RULE_MISSING` 422、`INSURANCE_LIMIT_EXCEEDED` 422。

## 7 运单查询

`GET /logistics/waybill/{waybillId}` 返回详情，`GET /logistics/waybill?page=1&size=10` 返回当前账号列表，按 `created_at` 倒序。`size` 上限 50。

不属于当前账号或不存在返回 404 `WAYBILL_NOT_FOUND`。

## 8 取消运单

`POST /logistics/waybill/{id}/cancel`，用户与员工共用同一接口，按调用者身份区分 `operator_type`。

```json
{"cancelReasonId":1,"cancelRemark":"不想要了"}
```

分流规则：

| 当前状态 | 目标状态 | 附带动作 |
| --- | --- | --- |
| 1 待揽收 | 6 已取消 | 无 |
| 2 已揽收 | 6 已取消 | 用户发起时调 auth 占位审核；员工发起不调 |
| 3 运输中 | 8 退回中 | 写 `waybill_return(return_reason_id=7)`，调占位审核（用户） |
| 7 异常 | 按最近一条异常的 `status_before`：1/2 → 6；3/4 → 8 并写退回单 | 调占位审核（用户） |

同一事务内还写：`waybill_cancel`、`approval(status=2)`、`waybill_trace(action=8)`、`event_outbox(event_type=3 业务终止, biz_type=2)`。

返回 `{"waybillId":"...","waybillNo":"FF...","status":6}`。

错误码：`WAYBILL_STATUS_INVALID` 409、`AUDIT_UNAVAILABLE` 502（占位审核不可用，整笔不写库）。

## 9 异常标记与解除

`POST /logistics/waybill/{id}/exception`

```json
{"exceptionReasonId":4,"exceptionRemark":"电话空号"}
```

允许标记的状态：`1/2/3/4`。写入 `waybill_exception`、轨迹动作码 7，运单置 7。仅在职员工，否则 403 `STAFF_FORBIDDEN`。

`POST /logistics/waybill/{id}/exception/resolve`：要求当前状态为 7，回到最近一条异常的 `status_before`，写轨迹动作码 13。仅在职员工。

## 10 轨迹查询

`GET /logistics/waybill/{id}/trace`，按 `occurred_at` 升序。

```json
[{"traceId":1,"actionId":8,"statusAfter":6,"operatorType":4,"operatorName":"张三",
  "occurredAt":"2026-11-10T10:00:00"}]
```

`actionId` 对应 `trace_action` 字典，名称由前端按字典渲染。运单归属人或在职员工可见，否则 404。

## 11 揽收与签收

`POST /logistics/waybill/{id}/pickup`：`1 → 2`，写 `picked_at` 与 `current_site_id`（寄件 adcode 归属网点）；员工所属网点必须与之一致，否则 403。

`POST /logistics/waybill/{id}/deliver`

```json
{"signType":2,"signName":"王五","signPhone":"13700000000"}
```

| signType | 含义 | 附加要求 |
| --- | --- | --- |
| 1 | 本人签收 | `signName`/`signPhone` 可空 |
| 2 | 他人代收 | 两者必填，否则 400 |

`4 → 5`，写 `delivered_at`、`sign_type`、`sign_name`、`sign_phone`，轨迹动作码 6 或 11。

`POST /logistics/waybill/{id}/deliver/fail`：状态保持 4，只写轨迹动作码 5。

## 12 交接单

```text
POST /logistics/handover
{"transportLineId":"...","vehicleId":null,"driverStaffId":null}
```

`from_site_id`/`to_site_id` 由线路带出，创建后 `status=0` 待发、`departed_at` 为 NULL。

```text
POST /logistics/handover/{id}/load
{"waybillIds":["...","..."]}
```

可分批提交，已在单内的运单幂等跳过。每票校验：

1. 运单状态为 `2已揽收`；
2. `current_site_id` 等于交接单 `from_site_id`；
3. `RouteResolver` 算出的下一跳线路等于本次交接单线路。

通过后写 `handover_item`、运单 `2 → 3`、轨迹动作码 3「离开」。

`POST /logistics/handover/{id}/depart`：`0 → 1`，写 `departed_at`；无装车运单时 409。

`POST /logistics/handover/{id}/arrive`：`1 → 2`，写 `arrived_at`；每票 `current_site_id` 更新为 `to_site_id`、轨迹动作码 2；若到达网点覆盖收件 adcode，运单自动 `3 → 4`，并写动作码 4。

全部接口要求在职员工，否则 403。

## 13 内部网点与发证

### 13.1 网点列表

`GET /logistics/internal/sites`，返回全部网点，不过滤 `enabled` 与 `site_type`。

```json
[{"siteId":"...","siteCode":"HZ-XH","siteName":"杭州西湖营业网点","siteType":1,"enabled":1}]
```

### 13.2 签发培训凭证

`POST /logistics/internal/staff-trainings`

```json
{"phone":"13900000001","staffRole":1,"siteCode":"HZ-XH"}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `phone` | 是 | 11 位手机号，用于在 auth 查账号 |
| `staffRole` | 是 | `1快递员 2客服 3司机`，无默认值 |
| `siteCode` | 是 | 网点编码，须存在 |

服务端规则：

1. 调 auth `GET /auth/internal/accounts/by-phone` 取账号与权威姓名
2. 姓名 `realName` 为空 → 422 `REAL_NAME_REQUIRED`
3. 同账号同岗位已有凭证 → 409 `VOUCHER_EXISTS`
4. 凭证码 12 位，字符集去掉 `0/O/1/I`，有效期 7 天
5. `issued_by_staff_name` 固定 `System`

成功后返回 201：

```json
{"staffTrainingId":"...","voucherCode":"A7K2M9QX3B5C","expireAt":"2026-11-19T15:30:00",
 "staffRole":1,"siteCode":"HZ-XH","siteName":"杭州西湖营业网点",
 "traineeName":"张三","traineeAccountId":"..."}
```

错误码：`INVALID_PARAM` 400、`ACCOUNT_NOT_FOUND` 404、`SITE_NOT_FOUND` 404、`REAL_NAME_REQUIRED` 422、`VOUCHER_EXISTS` 409、`AUTH_UNAVAILABLE` 502。

## 14 员工入职

`POST /logistics/staff/apply`，需 `Authorization: Bearer`。

```json
{"voucherCode":"A7K2M9QX3B5C","applyRemark":"希望入职杭州西湖网点"}
```

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `voucherCode` | 是 | 12 位凭证码，大小写不敏感 |
| `applyRemark` | 否 | ≤255，写入 `staff_apply.apply_remark` |

服务端在同一事务内：

1. 账号已有员工记录 → 409 `ALREADY_STAFF`
2. 凭证不存在 → 404 `VOUCHER_NOT_FOUND`
3. 凭证不合格或 `voucher_expire_at <= now()` → 422 `VOUCHER_EXPIRED`
4. 凭证归属账号与当前账号不一致 → 403 `VOUCHER_ACCOUNT_MISMATCH`
5. 工号 `KD-` + 8 位全局流水
6. 写 `logistics_staff`、`staff_apply`、`approval(status=2)`
7. `staff_training.voucher_expire_at = now()`，使用即失效
8. 调 auth `POST /auth/internal/roles/grant` 授予 `role_id=2`，失败整笔回滚
9. 写 `event_outbox(target_service=1, event_type=1, auth_role_id=2, biz_type=3)`

成功返回 201：

```json
{"staffId":"...","staffNo":"KD-00000001","staffName":"杨浚铠","staffRole":1,
 "siteCode":"HZ-XH","siteName":"杭州西湖营业网点","appliedAt":"2026-11-12T16:10:00"}
```

错误码：`INVALID_PARAM` 400、`ALREADY_STAFF` 409、`VOUCHER_NOT_FOUND` 404、`VOUCHER_EXPIRED` 422、`VOUCHER_ACCOUNT_MISMATCH` 403、`AUTH_UNAVAILABLE` 502。


