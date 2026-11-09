-- V1__init_logistics_schema.sql
-- 物流业务服务初始表结构（仅结构，不含种子数据）
-- 依据：草稿/运单表草稿.md、物流人员表草稿.md、计费规则草稿.md、转运路由草稿.md、决策日志 D-022/D-023

-- ============================================================
-- 1. 字典表
-- ============================================================

CREATE TABLE item_category (
  item_category_id  TINYINT UNSIGNED NOT NULL COMMENT '类别码，固定枚举，非自增',
  category_name     VARCHAR(32)      NOT NULL COMMENT '类别中文名，如文件/食品/数码',
  enabled           TINYINT          NOT NULL DEFAULT 1 COMMENT '1启用 0停用；App 下拉只取启用项',
  PRIMARY KEY (item_category_id),
  UNIQUE KEY uk_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='物品类别字典表';

CREATE TABLE trace_action (
  trace_action_id  TINYINT UNSIGNED NOT NULL COMMENT '动作码，固定枚举，非自增',
  action_name      VARCHAR(32)      NOT NULL COMMENT '动作名，如揽收/到达/派送/本人签收',
  PRIMARY KEY (trace_action_id),
  UNIQUE KEY uk_action_name (action_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='轨迹动作字典表';

CREATE TABLE cancel_reason (
  cancel_reason_id  TINYINT UNSIGNED NOT NULL COMMENT '原因码，固定枚举，非自增',
  reason_name       VARCHAR(32)      NOT NULL COMMENT '原因名，如「信息填写错误」',
  enabled           TINYINT          NOT NULL DEFAULT 1 COMMENT '1启用 0停用；取消页只展示启用项',
  PRIMARY KEY (cancel_reason_id),
  UNIQUE KEY uk_reason_name (reason_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='取消原因字典表';

CREATE TABLE exception_reason (
  exception_reason_id TINYINT UNSIGNED NOT NULL COMMENT '异常原因码，固定枚举，非自增',
  reason_name         VARCHAR(32)      NOT NULL COMMENT '原因名，如「违禁品」「超区」「空头物流」',
  enabled             TINYINT          NOT NULL DEFAULT 1 COMMENT '1启用 0停用；后台只展示启用项',
  PRIMARY KEY (exception_reason_id),
  UNIQUE KEY uk_reason_name (reason_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='异常原因字典表';

CREATE TABLE return_reason (
  return_reason_id TINYINT UNSIGNED NOT NULL COMMENT '退回原因码，固定枚举，非自增',
  reason_name      VARCHAR(32)      NOT NULL COMMENT '原因名，如「收件人拒收」「地址不详」',
  enabled          TINYINT          NOT NULL DEFAULT 1 COMMENT '1启用 0停用；后台只展示启用项',
  PRIMARY KEY (return_reason_id),
  UNIQUE KEY uk_reason_name (reason_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='退回原因字典表';

CREATE TABLE freight_relation (
  freight_relation_id TINYINT UNSIGNED NOT NULL COMMENT '关系码，固定枚举，非自增',
  relation_name       VARCHAR(32)      NOT NULL COMMENT '关系名，如「同城」「省内」「跨省」',
  PRIMARY KEY (freight_relation_id),
  UNIQUE KEY uk_relation_name (relation_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='寄收关系字典表';

-- ============================================================
-- 2. 运单与轨迹
-- ============================================================

CREATE TABLE waybill (
  waybill_id   CHAR(36)    NOT NULL COMMENT '运单主键，UUID',
  waybill_no   VARCHAR(20) NOT NULL COMMENT '对外单号：FF+yyMMdd+6位日序号，如 FF251105000042',
  auth_account_id   CHAR(36)    NOT NULL COMMENT '下单账号 → auth_account.auth_account_id；跨库只存值，不建外键',

  -- ===== 寄件人快照（不引用 user_address，防历史被改写）=====
  sender_name         VARCHAR(32)   NOT NULL COMMENT '寄件人姓名',
  sender_phone        VARCHAR(20)   NOT NULL COMMENT '寄件人手机号',
  sender_province     VARCHAR(32)   NOT NULL COMMENT '省',
  sender_city         VARCHAR(32)   NOT NULL COMMENT '市；直辖市以省填充',
  sender_district     VARCHAR(32)   NOT NULL COMMENT '区/县',
  sender_detail_addr  VARCHAR(200)  NOT NULL COMMENT '详细门牌，唯一可送达依据',
  sender_adcode       CHAR(6)       NOT NULL COMMENT '行政区划码；由 geo_address 带入，下单时必须非空',
  sender_poi_id       VARCHAR(64)   NOT NULL COMMENT '高德 POI ID；D-022 规定 POI 必选，故非空',
  sender_poi_name     VARCHAR(128)  NULL     COMMENT 'POI 名称，辅助',
  sender_lng          DECIMAL(10,6) NOT NULL COMMENT 'GCJ-02 经度；POI 必选故非空',
  sender_lat          DECIMAL(10,6) NOT NULL COMMENT 'GCJ-02 纬度；POI 必选故非空',

  -- ===== 收件人快照 =====
  receiver_name         VARCHAR(32)   NOT NULL COMMENT '收件人姓名',
  receiver_phone        VARCHAR(20)   NOT NULL COMMENT '收件人手机号',
  receiver_province     VARCHAR(32)   NOT NULL COMMENT '省',
  receiver_city         VARCHAR(32)   NOT NULL COMMENT '市；直辖市以省填充',
  receiver_district     VARCHAR(32)   NOT NULL COMMENT '区/县',
  receiver_detail_addr  VARCHAR(200)  NOT NULL COMMENT '详细门牌，唯一可送达依据',
  receiver_adcode       CHAR(6)       NOT NULL COMMENT '行政区划码；由 geo_address 带入，下单时必须非空',
  receiver_poi_id       VARCHAR(64)   NOT NULL COMMENT '高德 POI ID；D-022 规定 POI 必选，故非空',
  receiver_poi_name     VARCHAR(128)  NULL     COMMENT 'POI 名称，辅助',
  receiver_lng          DECIMAL(10,6) NOT NULL COMMENT 'GCJ-02 经度；POI 必选故非空',
  receiver_lat          DECIMAL(10,6) NOT NULL COMMENT 'GCJ-02 纬度；POI 必选故非空',

  -- ===== 物品（一票一件，直接内联）=====
  item_name         VARCHAR(64)      NOT NULL COMMENT '物品名称，用户填写',
  item_category_id  TINYINT UNSIGNED NULL     COMMENT '→ item_category.item_category_id；NULL 表示未分类',
  item_weight       DECIMAL(10,3)    NOT NULL COMMENT '下单时用户填写的计费重量 kg；不做体积重',
  declared_value    DECIMAL(10,2)    NULL     COMMENT '申报价值，用户可选填；NULL 表示未申报，不参与保价',

  -- ===== 金额（下单时算定的快照，不存计算过程）=====
  freight_amount    DECIMAL(10,2)  NOT NULL DEFAULT 0.00 COMMENT '运费',
  insured_fee       DECIMAL(10,2)  NOT NULL DEFAULT 0.00 COMMENT '保价费，未申报时为 0',
  total_amount      DECIMAL(10,2)  NOT NULL DEFAULT 0.00 COMMENT '应付总额 = 运费 + 保价费',
  price_rule_id     BIGINT UNSIGNED NULL    COMMENT '下单时使用的运费规则 → price_rule.price_rule_id；便于追溯费率',
  insurance_rule_id BIGINT UNSIGNED NULL    COMMENT '下单时使用的保价规则 → insurance_rule.insurance_rule_id；未申报保价时为 NULL',
  pay_method        TINYINT UNSIGNED NOT NULL COMMENT '1寄付 2到付；下单必选，决定支付单建单时机',

  -- ===== 状态（单列枚举，历史交给轨迹表）=====
  status  TINYINT NOT NULL DEFAULT 1 COMMENT '1待揽收 2已揽收 3运输中 4派送中 5已签收 6已取消 7异常 8退回中 9已退回',

  -- ===== 时间节点（中间节点由轨迹表推导；取消时间见取消表）=====
  picked_at     DATETIME NULL COMMENT '揽收时间',
  delivered_at  DATETIME NULL COMMENT '正常签收时间（状态5）；退回签收不写此字段，退回时间见 waybill_return.completed_at',
  created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  remark  VARCHAR(255) NULL COMMENT '用户备注',

  PRIMARY KEY (waybill_id),
  UNIQUE KEY uk_waybill_no (waybill_no),
  KEY idx_account_time (auth_account_id, created_at),
  KEY idx_status_time (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='运单主表';

CREATE TABLE waybill_trace (
  waybill_trace_id  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '轨迹主键，自增',
  waybill_id        CHAR(36)         NOT NULL COMMENT '→ waybill.waybill_id',
  operator_type     TINYINT UNSIGNED NOT NULL COMMENT '1系统 2物流人员 3网点 4用户',
  operator_id       CHAR(36)         NULL     COMMENT '操作主体ID；系统事件为 NULL',
  operator_name     VARCHAR(64)      NULL     COMMENT '操作者名称快照；系统事件为 NULL',
  trace_action_id   TINYINT UNSIGNED NOT NULL COMMENT '→ trace_action.trace_action_id，表达「在做什么」',
  status_after      TINYINT          NOT NULL COMMENT '事件后运单状态；未改变状态的事件填当前状态',
  occurred_at       DATETIME         NOT NULL COMMENT '业务发生时间',
  created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  PRIMARY KEY (waybill_trace_id),
  KEY idx_waybill_time (waybill_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='运单轨迹表（append-only）';

CREATE TABLE waybill_cancel (
  waybill_cancel_id  CHAR(36)         NOT NULL COMMENT '取消记录主键，UUID；每轮申请一条',
  waybill_id         CHAR(36)         NOT NULL COMMENT '→ waybill.waybill_id',
  apply_round        TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '申请轮次，从 1 起；驳回或撤回后再次申请递增',
  cancel_reason_id   TINYINT UNSIGNED NOT NULL COMMENT '→ cancel_reason.cancel_reason_id',
  cancel_remark      VARCHAR(255)     NULL     COMMENT '补充说明',

  operator_type      TINYINT UNSIGNED NOT NULL COMMENT '发起方：1系统 2物流人员 3网点 4用户',
  operator_id        CHAR(36)         NULL     COMMENT '发起方ID；系统事件为 NULL',
  operator_name      VARCHAR(64)      NULL     COMMENT '发起方名称快照',

  status_before      TINYINT          NOT NULL COMMENT '取消前运单状态，仅允许 1待揽收/2已揽收/3运输中/7异常',

  cancelled_at       DATETIME         NULL     COMMENT '生效时间；待审核、驳回或撤回时保持 NULL',
  created_at         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请即入库',
  updated_at         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (waybill_cancel_id),
  UNIQUE KEY uk_waybill_round (waybill_id, apply_round),
  KEY idx_reason_time (cancel_reason_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='运单取消记录表';

CREATE TABLE waybill_exception (
  waybill_exception_id CHAR(36)         NOT NULL COMMENT '异常记录主键 UUID',
  waybill_id           CHAR(36)         NOT NULL COMMENT '→ waybill.waybill_id',
  exception_reason_id  TINYINT UNSIGNED NOT NULL COMMENT '→ exception_reason.exception_reason_id',
  exception_remark     VARCHAR(255)     NULL     COMMENT '补充说明',

  operator_type        TINYINT UNSIGNED NOT NULL COMMENT '标记方：1系统 2物流人员 3网点 4用户；系统仅用于到付超时报备',
  operator_id          CHAR(36)         NULL     COMMENT '标记方ID',
  operator_name        VARCHAR(64)      NULL     COMMENT '标记方名称快照',

  status_before        TINYINT          NOT NULL COMMENT '标记前运单状态',
  created_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生即入库',
  updated_at           DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (waybill_exception_id),
  KEY idx_waybill_time (waybill_id, created_at),
  KEY idx_reason_time (exception_reason_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='运单异常记录表';

CREATE TABLE waybill_return (
  waybill_return_id CHAR(36)         NOT NULL COMMENT '退回记录主键 UUID',
  waybill_id        CHAR(36)         NOT NULL COMMENT '→ waybill.waybill_id',
  return_reason_id  TINYINT UNSIGNED NOT NULL COMMENT '→ return_reason.return_reason_id',
  return_remark     VARCHAR(255)     NULL     COMMENT '补充说明',

  operator_type     TINYINT UNSIGNED NOT NULL COMMENT '发起方：2物流人员 3网点 4用户',
  operator_id       CHAR(36)         NULL     COMMENT '发起方ID',
  operator_name     VARCHAR(64)      NULL     COMMENT '发起方名称快照',

  status_before     TINYINT          NOT NULL COMMENT '发起前运单状态，仅允许 3运输中/4派送中/7异常；3 只由取消联动写入',
  completed_at      DATETIME         NULL     COMMENT '退回签收时间，对应状态 9；未签收为 NULL',
  created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发起即入库',
  updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (waybill_return_id),
  KEY idx_waybill_time (waybill_id, created_at),
  KEY idx_reason_time (return_reason_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='运单退回记录表';

-- ============================================================
-- 3. 通用审核
-- ============================================================

CREATE TABLE approval (
  approval_id          CHAR(36)    NOT NULL COMMENT '审核记录主键，UUID',
  biz_type             TINYINT UNSIGNED NOT NULL COMMENT '业务类型（全库统一词表）：2运单取消单 3员工入职申请单',
  biz_id               CHAR(36)    NOT NULL COMMENT '业务记录主键：biz_type=2 时为 waybill_cancel.waybill_cancel_id；biz_type=3 时为 staff_apply.staff_apply_id',
  status               TINYINT     NOT NULL DEFAULT 1 COMMENT '1待审核 2通过 3驳回 4已撤回',

  apply_operator_type  TINYINT UNSIGNED NOT NULL COMMENT '申请人类型：1系统 2物流人员 3网点 4用户',
  apply_operator_id    CHAR(36)    NULL     COMMENT '申请人ID；系统事件为 NULL',
  apply_operator_name  VARCHAR(64) NULL     COMMENT '申请人名称快照',
  apply_remark         VARCHAR(255) NULL    COMMENT '申请说明',

  audit_operator_type  TINYINT UNSIGNED NULL COMMENT '审核人类型，同申请人词表',
  audit_operator_id    CHAR(36)    NULL     COMMENT '审核人ID',
  audit_operator_name  VARCHAR(64) NULL     COMMENT '审核人名称快照；系统审核时为 NULL',
  audit_remark         VARCHAR(255) NULL    COMMENT '审核意见，驳回时填原因',

  audited_at           DATETIME    NULL     COMMENT '审核完成时间；待审核为 NULL',
  created_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请即入库',
  updated_at           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (approval_id),
  UNIQUE KEY uk_biz (biz_type, biz_id),
  KEY idx_status_time (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='通用审核记录表';

-- ============================================================
-- 4. 网点与组织
-- ============================================================

CREATE TABLE logistics_site (
  logistics_site_id        CHAR(36)     NOT NULL COMMENT '网点主键，UUID；轨迹 operator_type=3 时作为 operator_id',
  site_code                VARCHAR(20)  NOT NULL COMMENT '对外网点编码，如 HZ-XH-01；唯一且不回收，停用后仍占用',
  site_name                VARCHAR(64)  NOT NULL COMMENT '网点名称；轨迹 operator_name 的取值来源',
  site_type                TINYINT UNSIGNED NOT NULL COMMENT '1营业网点(揽收+派送) 2转运中心 3驿站/自提点',
  parent_logistics_site_id CHAR(36)     NULL     COMMENT '上级网点 → logistics_site.logistics_site_id；转运中心为 NULL，营业网点指向转运中心，驿站指向营业网点',

  geo_address_id           CHAR(36)     NOT NULL COMMENT '→ geo_address.geo_address_id；省市区/adcode/坐标由缓存提供',
  detail_addr              VARCHAR(200) NOT NULL COMMENT '营业场所门牌，网点唯一可送达依据',

  contact_phone            VARCHAR(20)  NULL     COMMENT '对外联系电话',
  leader_name              VARCHAR(32)  NULL     COMMENT '当前负责人姓名；与 logistics_staff_id 在变更负责人时同一次写入',
  logistics_staff_id       CHAR(36)     NULL     COMMENT '负责人 → logistics_staff.logistics_staff_id；待数据回填',

  enabled    TINYINT  NOT NULL DEFAULT 1 COMMENT '1启用 0停用；不删行，历史轨迹仍需渲染',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (logistics_site_id),
  UNIQUE KEY uk_site_code (site_code),
  KEY idx_type (site_type),
  KEY idx_parent (parent_logistics_site_id),
  KEY idx_geo (geo_address_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='物流网点表（营业网点/转运中心/驿站）';

CREATE TABLE logistics_site_area (
  logistics_site_area_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分区主键，自增；纯关联表，无可单独引用的业务身份',
  logistics_site_id      CHAR(36)        NOT NULL COMMENT '→ logistics_site.logistics_site_id，必须是 site_type=1 的营业网点',
  adcode                 CHAR(6)         NOT NULL COMMENT '揽派责任区县码；一个区县只归属一个营业网点',
  created_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at             DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (logistics_site_area_id),
  UNIQUE KEY uk_adcode (adcode),
  KEY idx_site (logistics_site_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='营业网点揽派责任区表（区县 → 营业网点，1:N）';

CREATE TABLE logistics_staff (
  logistics_staff_id CHAR(36)    NOT NULL COMMENT '人员主键 UUID；轨迹 operator_type=2 时作为 operator_id',
  auth_account_id    CHAR(36)    NOT NULL COMMENT '→ auth_account.auth_account_id；跨库只存值',
  staff_no           VARCHAR(16) NOT NULL COMMENT '工号：岗位前缀-8位全局流水，如 KD-00000001；唯一且不回收，转岗不改',
  staff_name         VARCHAR(32) NOT NULL COMMENT '姓名快照，入职时取自 auth_user_profile.real_name；由 auth_outbox 姓名变更事件同步',
  staff_phone        VARCHAR(20) NOT NULL COMMENT '手机号快照，入职时取自 auth_account.phone；由 auth_outbox 手机号变更事件同步',
  staff_role         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1快递员(揽收+派送) 2客服 3司机；当前实际只出现 1',
  logistics_site_id  CHAR(36)    NOT NULL COMMENT '归属网点 → logistics_site.logistics_site_id',

  enabled    TINYINT  NOT NULL DEFAULT 1 COMMENT '1在职 0离职；不删行，历史轨迹仍需渲染',
  hired_at   DATETIME NOT NULL COMMENT '当前雇佣段开始时间；重新入职时复位，历史见离职日志表',
  left_at    DATETIME NULL     COMMENT '离职时间；在职为 NULL，重新入职时清空',

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (logistics_staff_id),
  UNIQUE KEY uk_auth_account (auth_account_id),
  UNIQUE KEY uk_staff_no (staff_no),
  KEY idx_site_role (logistics_site_id, staff_role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='物流人员表';

CREATE TABLE staff_training (
  staff_training_id       CHAR(36)    NOT NULL COMMENT '培训记录主键 UUID',
  logistics_site_id       CHAR(36)    NOT NULL COMMENT '组织培训的网点 → logistics_site.logistics_site_id；也是入职后的归属网点',
  trainee_auth_account_id CHAR(36)    NOT NULL COMMENT '学员账号 → auth_account.auth_account_id；凭证核验依据',
  trainee_name            VARCHAR(32) NOT NULL COMMENT '学员姓名，培训时登记',
  trainee_phone           VARCHAR(20) NULL     COMMENT '联系手机号，仅展示，不参与核验',

  trained_at              DATETIME    NOT NULL COMMENT '培训完成时间',
  result                  TINYINT UNSIGNED NOT NULL COMMENT '1合格 2不合格',
  voucher_code            VARCHAR(32) NULL     COMMENT '培训凭证码，全局唯一；仅合格时生成，建议 12 位随机字母数字',
  voucher_expire_at       DATETIME    NULL     COMMENT '凭证有效期',
  voucher_used_at         DATETIME    NULL     COMMENT '核销时间；NULL 表示未使用，一次性；事务失败由数据库回滚，核销后不可复用',
  issued_by_staff_id      CHAR(36)    NULL     COMMENT '录入/发证人 → logistics_staff.logistics_staff_id',
  issued_by_staff_name    VARCHAR(32) NULL     COMMENT '发证人姓名快照，录入时写入，之后不随改名更新',

  created_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (staff_training_id),
  UNIQUE KEY uk_voucher (voucher_code),
  KEY idx_site_time (logistics_site_id, trained_at),
  KEY idx_account (trainee_auth_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='网点培训与凭证表';

CREATE TABLE staff_apply (
  staff_apply_id    CHAR(36)     NOT NULL COMMENT '申请单主键 UUID',
  auth_account_id    CHAR(36)     NOT NULL COMMENT '申请人账号 → auth_account.auth_account_id',
  staff_training_id CHAR(36)     NOT NULL COMMENT '→ staff_training.staff_training_id；归属网点由培训记录决定',
  apply_staff_role  TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '申请岗位：当前只允许 1快递员(揽收+派送)；客服/司机预留',
  apply_remark      VARCHAR(255) NULL     COMMENT '申请人自述',

  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (staff_apply_id),
  KEY idx_account_time (auth_account_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='员工入职申请表';

-- ============================================================
-- 5. 路由与车辆
-- ============================================================

CREATE TABLE transport_line (
  transport_line_id CHAR(36)         NOT NULL COMMENT '线路主键 UUID',
  line_code         VARCHAR(24)      NOT NULL COMMENT '线路编码，如 ZX-HZ-SH-01；唯一且不回收',
  origin_site_id    CHAR(36)         NOT NULL COMMENT '起点网点 → logistics_site.logistics_site_id',
  dest_site_id      CHAR(36)         NOT NULL COMMENT '终点网点 → logistics_site.logistics_site_id',
  line_type         TINYINT UNSIGNED NOT NULL COMMENT '1支线(营业网点↔转运中心) 2干线(转运中心↔转运中心)',
  transit_hours     SMALLINT UNSIGNED NOT NULL COMMENT '标准时效，小时',
  enabled           TINYINT          NOT NULL DEFAULT 1 COMMENT '1启用 0停用；不删行，历史交接单仍需渲染',
  created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (transport_line_id),
  UNIQUE KEY uk_line_code (line_code),
  UNIQUE KEY uk_origin_dest (origin_site_id, dest_site_id, line_type),
  KEY idx_origin (origin_site_id, enabled),
  KEY idx_dest (dest_site_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='运输线路表';

CREATE TABLE route_rule (
  route_rule_id      CHAR(36)     NOT NULL COMMENT '规则主键 UUID',
  current_site_id    CHAR(36)     NOT NULL COMMENT '当前网点 → logistics_site.logistics_site_id；只允许 site_type=1/2',
  dest_adcode_prefix VARCHAR(6)   NOT NULL COMMENT '目的地区划前缀，如 33（省）、3301（市）、330106（区县）',
  transport_line_id  CHAR(36)     NOT NULL COMMENT '下一跳线路 → transport_line.transport_line_id',
  enabled            TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (route_rule_id),
  UNIQUE KEY uk_site_prefix (current_site_id, dest_adcode_prefix),
  KEY idx_line (transport_line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='路由规则表';

CREATE TABLE logistics_site_merge (
  logistics_site_merge_id CHAR(36)     NOT NULL COMMENT '撤并记录主键 UUID',
  from_site_id            CHAR(36)     NOT NULL COMMENT '被撤并网点 → logistics_site.logistics_site_id',
  to_site_id              CHAR(36)     NOT NULL COMMENT '并入网点 → logistics_site.logistics_site_id',
  merged_at               DATETIME     NOT NULL COMMENT '撤并生效时间',
  operator_name           VARCHAR(64)  NULL     COMMENT '操作人姓名快照',
  created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (logistics_site_merge_id),
  KEY idx_from_time (from_site_id, merged_at),
  KEY idx_to (to_site_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='网点撤并记录表';

CREATE TABLE vehicle (
  vehicle_id        CHAR(36)         NOT NULL COMMENT '车辆主键 UUID',
  plate_no          VARCHAR(16)      NOT NULL COMMENT '车牌号，唯一；含新能源 8 位牌',
  vehicle_type      TINYINT UNSIGNED NOT NULL COMMENT '1面包车 2厢式货车 3卡车',
  load_weight       DECIMAL(10,3)    NOT NULL COMMENT '核定载重 kg',
  logistics_site_id CHAR(36)         NOT NULL COMMENT '归属网点 → logistics_site.logistics_site_id；支线车归营业网点，干线车归转运中心',
  status            TINYINT          NOT NULL DEFAULT 1 COMMENT '1可用 2维修 3停用；停用不删行',
  created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (vehicle_id),
  UNIQUE KEY uk_plate_no (plate_no),
  KEY idx_site_status (logistics_site_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='公司车辆表';

CREATE TABLE handover (
  handover_id       CHAR(36)     NOT NULL COMMENT '交接单主键 UUID',
  handover_no       VARCHAR(24)  NOT NULL COMMENT '交接单号：HO+yyMMdd+6位日序号',
  transport_line_id CHAR(36)     NOT NULL COMMENT '→ transport_line.transport_line_id',
  direction         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '运输方向：1正向 2退回',
  from_site_id      CHAR(36)     NOT NULL COMMENT '交出网点快照 → logistics_site.logistics_site_id',
  to_site_id        CHAR(36)     NOT NULL COMMENT '接收网点快照 → logistics_site.logistics_site_id',
  vehicle_id        CHAR(36)     NULL     COMMENT '→ vehicle.vehicle_id；无车运输为空',
  vehicle_plate     VARCHAR(16)  NULL     COMMENT '车牌快照，发车时写入，之后不随车辆换牌更新',
  driver_staff_id   CHAR(36)     NULL     COMMENT '司机 → logistics_staff.logistics_staff_id；渠道未开放时为空',
  driver_name       VARCHAR(32)  NULL     COMMENT '司机姓名快照',
  operator_name     VARCHAR(64)  NULL     COMMENT '交接人姓名快照；与司机可能不是同一人',
  departed_at       DATETIME     NOT NULL COMMENT '发车时间',
  arrived_at        DATETIME     NULL     COMMENT '到达时间；在途为 NULL',
  status            TINYINT      NOT NULL DEFAULT 1 COMMENT '1在途 2已到达',
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (handover_id),
  UNIQUE KEY uk_handover_no (handover_no),
  KEY idx_line_time (transport_line_id, departed_at),
  KEY idx_status (status),
  KEY idx_from_time (from_site_id, departed_at),
  KEY idx_to_time (to_site_id, departed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='交接单表';

CREATE TABLE handover_item (
  handover_item_id CHAR(36) NOT NULL COMMENT '明细主键 UUID',
  handover_id      CHAR(36) NOT NULL COMMENT '→ handover.handover_id',
  waybill_id       CHAR(36) NOT NULL COMMENT '→ waybill.waybill_id',
  loaded_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '装车时间',
  PRIMARY KEY (handover_item_id),
  UNIQUE KEY uk_handover_waybill (handover_id, waybill_id),
  KEY idx_waybill_time (waybill_id, loaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='交接单明细表';

-- ============================================================
-- 6. 计费规则
-- ============================================================

CREATE TABLE price_rule (
  price_rule_id       BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '规则主键，自增；改价新建一条，不 UPDATE 历史行',
  freight_relation_id TINYINT UNSIGNED NOT NULL COMMENT '→ freight_relation.freight_relation_id',
  first_weight        DECIMAL(10,3)    NOT NULL COMMENT '首重重量 kg，如 1.000',
  first_price         DECIMAL(10,2)    NOT NULL COMMENT '首重价格 元',
  extra_unit          DECIMAL(10,3)    NOT NULL COMMENT '续重单位 kg，如 0.500',
  extra_price         DECIMAL(10,2)    NOT NULL COMMENT '每续重单位价格 元',
  enabled             TINYINT          NOT NULL DEFAULT 1 COMMENT '1启用 0停用；同一关系只允许一条启用',
  created_at          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (price_rule_id),
  KEY idx_relation_enabled (freight_relation_id, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='运费规则表';

CREATE TABLE insurance_rule (
  insurance_rule_id  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '规则主键，自增；改价新建一条',
  rate               DECIMAL(6,4)    NOT NULL COMMENT '保价费率，如 0.0050 表示 0.5%',
  min_fee            DECIMAL(10,2)   NOT NULL COMMENT '最低保价费 元',
  max_declared_value DECIMAL(10,2)   NULL     COMMENT '最高申报价值 元；NULL 表示不限',
  enabled            TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用；全局只允许一条启用',
  created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (insurance_rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='保价规则表';

-- ============================================================
-- 7. 地址
-- ============================================================

CREATE TABLE geo_address (
  geo_address_id CHAR(36)      NOT NULL COMMENT '地理缓存主键 UUID',
  poi_id         VARCHAR(64)   NOT NULL COMMENT '高德 POI ID，缓存键',
  poi_name       VARCHAR(128)  NULL     COMMENT 'POI 名称，如「XX小区」',
  adcode         CHAR(6)       NOT NULL COMMENT '高德行政区划码；反查失败时拒绝入库',
  province       VARCHAR(32)   NULL     COMMENT '省',
  city           VARCHAR(32)   NULL     COMMENT '市；直辖市/省直辖县高德返回空，入库时以 province 填充',
  district       VARCHAR(32)   NULL     COMMENT '区/县',
  lng            DECIMAL(10,6) NOT NULL COMMENT 'GCJ-02 经度，取自 POI 坐标',
  lat            DECIMAL(10,6) NOT NULL COMMENT 'GCJ-02 纬度，取自 POI 坐标',
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (geo_address_id),
  UNIQUE KEY uk_poi (poi_id),
  KEY idx_adcode (adcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='地址地理缓存';

CREATE TABLE user_address (
  user_address_id CHAR(36)     NOT NULL COMMENT '用户地址条目 UUID',
  auth_account_id CHAR(36)     NOT NULL COMMENT '→ auth_account.auth_account_id；跨库只存值，不建外键',
  geo_address_id  CHAR(36)     NOT NULL COMMENT '→ geo_address.geo_address_id',
  contact_name    VARCHAR(32)  NOT NULL COMMENT '联系人姓名',
  contact_phone   VARCHAR(20)  NOT NULL COMMENT '联系人手机号',
  detail_addr     VARCHAR(200) NOT NULL COMMENT '栋/单元/楼层/门牌',
  tag             VARCHAR(16)  NULL     COMMENT '家/公司/学校，仅展示',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted_at      DATETIME     NULL     COMMENT '软删除；复活时置空',
  PRIMARY KEY (user_address_id),
  UNIQUE KEY uk_account_recipient (auth_account_id, geo_address_id, detail_addr, contact_name, contact_phone),
  KEY idx_geo (geo_address_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='用户地址簿';

-- ============================================================
-- 8. 跨库事件
-- ============================================================

CREATE TABLE event_outbox (
  event_outbox_id BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '事件主键，自增',
  target_service  TINYINT UNSIGNED NOT NULL COMMENT '目标服务：1认证 3支付；服务码全库统一',
  event_type      TINYINT UNSIGNED NOT NULL COMMENT '本表独立事件词表：1授予角色 2撤销角色 3业务终止（关单+退款）；不与 payment_outbox 共用',
  auth_account_id CHAR(36)         NULL     COMMENT '目标账号 → auth_account.auth_account_id；仅 auth 事件填写',
  auth_role_id    TINYINT UNSIGNED NULL     COMMENT '目标角色码，对应 auth_role.auth_role_id；仅 auth 事件填写',
  biz_type        TINYINT UNSIGNED NOT NULL COMMENT '来源业务（全库统一词表）：1运单 2运单取消单 3员工入职申请单 4员工离职',
  biz_id          CHAR(36)         NOT NULL COMMENT '来源业务主键：biz_type=1 为 waybill_id，2 为 waybill_cancel_id，3 为 staff_apply_id，4 为 logistics_staff_id',
  waybill_id      CHAR(36)         NULL     COMMENT '关联运单 → waybill.waybill_id；业务终止事件（event_type=3）必填，供支付服务按 waybill_id 定位运单支付单',
  status          TINYINT          NOT NULL DEFAULT 1 COMMENT '1待投递 2已投递 3待重试 4已放弃',
  retry_count     TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '已重试次数',
  next_retry_at   DATETIME         NULL     COMMENT '下次重试时间；待投递为 NULL',
  last_error      VARCHAR(255)     NULL     COMMENT '最近一次失败原因',
  created_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (event_outbox_id),
  KEY idx_status_next (status, next_retry_at),
  KEY idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci
  ROW_FORMAT=DYNAMIC COMMENT='跨库事件投递表（Outbox）';
