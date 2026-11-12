# auth-server 待办

状态：有效（产出物，唯一待办入口之一）
用途：认证服务未完成事项；接任务前读，完成即删。
更新时间：2026-11-04

编号：`S-` 安全、`F-` 功能、`E-` 工程、`L-` 长期。

## A 收尾必做

- [ ] `E-01` 端到端验收清单：7 个接口正常路径 + 6 个错误码路径
      （`PWD_WRONG`、`SMS_WRONG`、`SMS_EXPIRED`、`NO_ACCOUNT`、`PHONE_EXISTS`、`RATE_LIMITED`）。
      当前无 `src/test`，只能手工验证。

- [ ] `E-02` 修订 `规范/项目规范.md` 三处过期描述：§3「refresh 接口尚未实现」、
      §5「mapper、service、dto 待建」、§8「Redis 键设计（登录未实现，规划中）」。

- [ ] `E-03` 操作手册补「Android 联调」章节：局域网地址填写、明文 HTTP、
      模拟器 `10.0.2.2` 与真机局域网 IP 的差异。

- [ ] `E-04` 生产化配置说明：`auth.cookie.secure=false` 上线前改 `true`，并整体改走 HTTPS。

## B 功能缺口

- [ ] `S-01` 登录失败限流：密码登录无失败次数与 IP 频率限制，目前只有短信 60 秒冷却。

- [ ] `S-02` 登出补吊销：只带 refresh cookie、不带 `Authorization` 时，
      会话被关闭但 Redis 中的 access 仍有效，最长 2 小时。

- [ ] `S-03` 令牌响应补 `Cache-Control: no-store`，避免 access token 被中间层缓存。

- [ ] `F-03` 改密 / 重置密码接口：递增 `pwd_version` 让旧会话失效。
      当前 `pwd_version` 恒为 0，该设计从未被触发。

- [ ] `F-04` 账号状态管理接口：冻结 / 注销 / 恢复。
      当前只有状态读取与登录时判断，变更只能直接改库。

- [ ] `F-05` 角色撤销接口：授予已实现（`POST /auth/internal/roles/grant`），
      撤销无入口；`2 物流员工`、`3 驿站管理员` 的授予已由入职流程覆盖。

- [ ] `F-06` 过期会话落状态：`expire_at` 到期后行仍为 `status=1`，
      需定时任务标成 4（过期）。`SESSION_EXPIRED` 目前只在改密版本不一致时使用。

- [ ] `F-07` 登录日志清理与查询：`auth_login_log` 只写不读，无归档、无后台查询接口。

- [ ] `F-08` 用户资料读写接口：`GET /auth/profile`、`PUT /auth/profile`。
      表 `auth_user_profile` 尚未建，注册时也未插入空资料行，见 `草稿/auth扩展草稿.md`。

- [ ] `F-09` 内部接口鉴权：`/auth/internal/**` 目前无鉴权方案。
      候选：固定 `X-Internal-Token`、服务间签名、网络隔离，三项未选。

- [ ] `F-10` 手机号变更接口 `PUT /auth/phone`：旧凭证验证 + 新手机号验证码。
      未实现前 `auth_outbox.event_type=2` 无触发源。

- [ ] `F-11` 头像上传：当前 `avatar_url` 只存 URL，存储位置、大小限制、格式校验未定。

## C 工程与长期

- [ ] `E-05` 自动化测试脚手架：引入测试依赖并补关键路径用例。
      涉及依赖下载，执行前需用户授权。

- [ ] `L-01` 请求链路 ID 与 Actuator 指标：目前排障只能靠 `/auth/health` 与库表日志。

- [ ] `F-01` 会话表补充设备信息：`auth_session` 增加 `client_type`、`device_id`、`device_name`，
      用于支持「在线设备列表」与「按设备单独踢下线」。
      当前仅保留 `account_id`，只能按账号整体下线。
      关联影响：登录接口需采集设备标识，网关校验返回值需携带设备信息，
      前端需提供「登录设备管理」页面。

## D 设计未决

标注同 `快递业务待办.md`。均不阻塞建表。

- [ ] `[待裁决]` 手机号变更后的会话处置：旧 refresh 是否立即失效。
- [ ] `[待裁决]` 资料更新是否单独记日志（`auth_profile_log`），当前改名无审计。
- [ ] `[默认]` 入职申请被驳回后重新申请，从 auth 重新取姓名，不复用上次快照。
