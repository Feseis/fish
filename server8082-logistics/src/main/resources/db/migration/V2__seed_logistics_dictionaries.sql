-- V2__seed_logistics_dictionaries.sql
-- 物流字典种子数据

INSERT INTO item_category (item_category_id, category_name, enabled) VALUES
  (1, '文件证件', 1),
  (2, '食品',     1),
  (3, '数码产品', 1),
  (4, '服饰鞋帽', 1),
  (5, '日用品',   1),
  (6, '其他',     1);

INSERT INTO trace_action (trace_action_id, action_name) VALUES
  (1,  '揽收'),
  (2,  '到达网点/转运中心'),
  (3,  '离开网点/转运中心'),
  (4,  '派送中'),
  (5,  '投递失败'),
  (6,  '本人签收'),
  (7,  '异常'),
  (8,  '取消'),
  (9,  '退回'),
  (10, '退回签收'),
  (11, '他人代收'),
  (12, '驿站签收'),
  (13, '异常解除');

INSERT INTO cancel_reason (cancel_reason_id, reason_name, enabled) VALUES
  (1, '用户主动取消（不需要了）',   1),
  (2, '信息填写错误（地址/电话）', 1),
  (3, '重复下单',                   1),
  (4, '超时未揽收',                 1),
  (5, '违禁品/非法物品',            1),
  (6, '超区无法揽收',               1),
  (7, '其他',                       1);

INSERT INTO exception_reason (exception_reason_id, reason_name, enabled) VALUES
  (1, '违禁品/非法物品',           1),
  (2, '超区无法派送',               1),
  (3, '地址不详',                   1),
  (4, '联系不上收件人',             1),
  (5, '包装破损',                   1),
  (6, '重量异常',                   1),
  (7, '长期无人受理（空头物流）',   1),
  (8, '到付超时未付',               1),
  (9, '其他',                       1);

INSERT INTO return_reason (return_reason_id, reason_name, enabled) VALUES
  (1, '收件人拒收',         1),
  (2, '地址不详无法投递',   1),
  (3, '多次投递失败',       1),
  (4, '到付拒付',           1),
  (5, '违禁品退回',         1),
  (6, '其他',               1),
  (7, '运单取消退回',       1);

INSERT INTO freight_relation (freight_relation_id, relation_name) VALUES
  (1, '同城'),
  (2, '省内'),
  (3, '跨省');
