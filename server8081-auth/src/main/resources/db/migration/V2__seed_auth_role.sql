-- V2__seed_auth_role.sql
-- 角色字典种子数据

INSERT INTO auth_role (role_id, role_name, description) VALUES
  (1, '收寄用户',   '发起寄件、查询物流状态的终端用户'),
  (2, '物流人员',   '执行揽收、运输、派送并上报物流状态'),
  (3, '驿站管理员', '负责末端驿站的入库、出库与暂存管理');
