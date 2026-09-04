SET NAMES utf8mb4;
-- ============================================================
-- 05-seed-demo-data.sql  — 演示数据修正与补齐（幂等，可重复执行）
-- 目标：让宽带业务演示数据的「状态关联 / 时间分布」更合理，
--       并让看板今日口径有真实可看的数字。
-- 说明：所有 UPDATE 都以 BBDEMO 前缀订单号为锚点，重复执行无副作用。
-- ============================================================

USE `bbpms`;

-- 1) 修正：订单 1003 为「已派单」，但关联工单 2003 却是 AUTO_CANCELLED（状态矛盾）
--    → 将工单还原为 DISPATCHED，并补派单时间 / 装维 / 期望完成时间
UPDATE work_order wo
JOIN broadband_order o ON o.id = wo.order_id
SET wo.status = 'DISPATCHED',
    wo.dispatch_time = o.dispatch_time,
    wo.installer_id  = 7,               -- install2（在岗可接单）
    wo.expected_finish_time = DATE_ADD(NOW(), INTERVAL 4 HOUR)
WHERE o.order_no = 'BBDEMO20260003' AND wo.status = 'AUTO_CANCELLED';

-- 2) 补宽带订单侧的派单时间与工单侧时间（1002/1004 已派发的节点）
UPDATE broadband_order o
JOIN work_order wo ON wo.order_id = o.id
SET wo.dispatch_time = COALESCE(wo.dispatch_time, DATE_SUB(o.create_time, INTERVAL -20 MINUTE))
WHERE o.order_no IN ('BBDEMO20260002','BBDEMO20260004');

-- 3) 今日完成演示：1005 订单 FINISHED，把完成节点前移到今天，支撑「今日完成/趋势」
UPDATE broadband_order o
JOIN work_order wo ON wo.order_id = o.id
SET o.completed_time = CONCAT(CURDATE(), ' 09:20:00'),
    wo.finish_time   = CONCAT(CURDATE(), ' 09:20:00')
WHERE o.order_no = 'BBDEMO20260005';

-- 4) 待审核订单的客服归属与备注（演示 RBAC/状态标签用）
UPDATE broadband_order o
JOIN sys_user u ON u.username = 'cs1'
SET o.cs_id = u.id
WHERE o.order_no IN ('BBDEMO20260001','BBDEMO20260008') AND o.cs_id IS NULL;

-- 5) 停滞工单演示：新增「安装中订单 + STALLED 工单」演示对（不破坏既有 IN_PROGRESS 覆盖）
INSERT INTO broadband_order
  (id, order_no, customer_id, package_code, package_name, install_address,
   status, cs_id, dispatch_time, completed_time, create_time, update_time, create_by, update_by, deleted, version)
SELECT 1099, 'BBDEMO20260099', c.id, 'FIBER_500M', '光纤500M', '上海市浦东新区演示路99号',
       'INSTALLING', NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR), NULL,
       DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 2, 2, 0, 0
FROM customer c LIMIT 1
ON DUPLICATE KEY UPDATE status = 'INSTALLING';

INSERT INTO work_order
  (id, work_no, order_id, installer_id, dispatcher_id, status,
   dispatch_time, accept_time, start_time, install_address, customer_phone,
   create_time, update_time, create_by, update_by, deleted, version,
   priority, expected_finish_time, last_active_at, stall_reason, cancel_type)
SELECT 9011, 'WODEMO20260901', 1099, 9, 4, 'STALLED',
       DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR),
       '上海市浦东新区演示路99号', '13900009999',
       DATE_SUB(NOW(), INTERVAL 6 HOUR), NOW(), 4, 4, 0, 0,
       1, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR), '现场缺少入户设备，待备货', NULL
FROM dual
ON DUPLICATE KEY UPDATE status = 'STALLED';

-- ============================================================
-- 06) 数据权限演示数据（DEPT / DEPT_AND_CHILD 验证）
-- 目标：构建「部门隔离」的可验证场景。
--  - 部门 2 Operations Dept（/1/2/）  → 现有 cs1..install5 都在此
--  - 部门 3 Field Ops A（/1/3/）      → 属 Operations Dept 的子部门（同分支）
--  - 部门 4 平行分支（/1/4/）         → 用 dispatch 演示「跨分支不可见」
--  - 部门 5 更深的子部门（/1/3/5/）   → 验证 DEPT_AND_CHILD 递归
-- 说明：权限键用 DUPLICATE KEY 幂等；业务数据用固定主键 ON DUPLICATE 幂等。
--       所有演示订单 create_by 均显式赋给某部门用户，便于验证按部门过滤。
-- ============================================================

-- 部门：2 已存在于 04；补 3/4/5
INSERT IGNORE INTO `sys_dept` (`id`, `parent_id`, `name`, `leader`, `phone`, `path`, `sort`, `status`) VALUES
(3, 1, 'Field Ops A',  'disp1', '13800000003', '/1/3/',   2, 1),
(4, 1, 'Branch B',     'cs2',   '13800000004', '/1/4/',   3, 1),
(5, 3, 'Sub Team 5',   'instA', '13800000005', '/1/3/5/', 1, 1);

-- 用户：补各部门的代表账号（均在各自部门）
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `dept_id`, `user_type`, `status`) VALUES
(11, 'disp2',    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Dispatcher B', '13800000021', 3, 4, 1),
(12, 'audit2',   '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Auditor B',    '13800000011', 3, 3, 1),
(13, 'disp3',    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Dispatcher C', '13800000022', 4, 4, 1),
(14, 'disp4',    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Dispatcher D', '13800000023', 5, 4, 1);

-- disp2/audit2/disp3 挂到各自角色（幂等）
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(11, 4), (12, 3), (13, 4), (14, 7);

-- ============================================================
-- 07) 数据权限测试角色（DEPT / DEPT_AND_CHILD 专用，避免污染现有角色）
-- selectUserDataScope 取 MAX(r.data_scope)，值越大范围越小（4=SELF > 3 > 2 > 1）
-- 所以测试账号必须只挂一个数据权限角色，否则 MAX 会让 scope 退化成 SELF。
-- 角色 7  DISPATCHER_DEPT  → data_scope=2 (DEPT)
-- 角色 8  AUDITOR_CHILD    → data_scope=3 (DEPT_AND_CHILD)
-- 复用 DISPATCHER / AUDITOR 的菜单权限（照抄 04 的 role_menu 绑定）
-- ============================================================
INSERT IGNORE INTO `sys_role` (`id`, `code`, `name`, `data_scope`, `status`, `sort`, `remark`) VALUES
(7, 'DISPATCHER_DEPT', '调度员(本部门)', 2, 1, 7, 'Data-scope DEPT test role'),
(8, 'AUDITOR_CHILD',   '审核员(本部门及下级)', 3, 1, 8, 'Data-scope DEPT_AND_CHILD test role');

-- 角色 7 菜单 = 角色 4（DISPATCHER）全部
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 7, menu_id FROM `sys_role_menu` WHERE role_id = 4;

-- 角色 8 菜单 = 角色 3（AUDITOR）全部
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 8, menu_id FROM `sys_role_menu` WHERE role_id = 3;

-- 测试账号只保留数据权限角色（先删默认角色绑定，避免 MAX(scope) 退化）
DELETE FROM `sys_user_role` WHERE user_id IN (11, 12);
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(11, 7), (12, 8);

-- ============================================================
-- 跨部门订单 / 工单
-- dept2 已有 BBDEMO 系列，create_by 全为 NULL（系统/种子兜底，可见所有人）
-- 这里再补：
--   2010 dept3（create_by=12 audit2）
--   2020 dept3 子部门 dept5（create_by=13 ？实际用所属部门；装维用不到订单列表，
--        为验证 DEPT_AND_CHILD 递归给 audit2(scope=3, dept3) 看 dept3+dept5，同时 disp1(dept2,scope=2) 看不见）
--   2030 dept4 平行分支（create_by=13 disp3）
-- ============================================================

INSERT IGNORE INTO `broadband_order`
    (`id`, `order_no`, `customer_id`, `package_code`, `package_name`, `install_address`,
     `expected_install_date`, `status`, `cs_id`, `auditor_id`, `audit_time`, `audit_remark`,
     `create_time`, `update_time`, `create_by`, `update_by`, `deleted`, `version`) VALUES
(2010, 'BBDEMO20260201', 2, 'PKG_500M', '500M Broadband', '北京市海淀区部门3路1号', NOW() + INTERVAL 2 DAY, 'CREATED', 2, NULL, NULL, NULL, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, 12, 12, 0, 0),
(2020, 'BBDEMO20260202', 4, 'PKG_300M', '300M Broadband', '北京市昌平区部门5路2号', NOW() + INTERVAL 3 DAY, 'CREATED', 2, NULL, NULL, NULL, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR, 11, 11, 0, 0),
(2030, 'BBDEMO20260203', 3, 'PKG_1G',   '1G Broadband',   '北京市朝阳区部门4路3号', NOW() + INTERVAL 1 DAY, 'CREATED', 2, NULL, NULL, NULL, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 4 HOUR, 13, 13, 0, 0),
(2040, 'BBDEMO20260204', 1, 'PKG_500M', '500M Broadband', '北京市东城区部门5子队4号', NOW() + INTERVAL 1 DAY, 'CREATED', 2, NULL, NULL, NULL, NOW() - INTERVAL 5 HOUR, NOW() - INTERVAL 5 HOUR, 14, 14, 0, 0);

INSERT IGNORE INTO `work_order`
    (`id`, `work_no`, `order_id`, `installer_id`, `dispatcher_id`, `status`, `dispatch_time`,
     `create_time`, `update_time`, `create_by`, `update_by`, `deleted`, `version`,
     `priority`, `expected_finish_time`, `install_address`, `customer_phone`, `package_name`) VALUES
(2010, 'WODEMO20260201', 2010, 6, 12, 'DISPATCHED', NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 1 HOUR, 12, 12, 0, 0, 3, NOW() + INTERVAL 1 DAY, '北京市海淀区部门3路1号', '13900000002', '500M Broadband'),
(2020, 'WODEMO20260202', 2020, 7, 11, 'DISPATCHED', NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 2 HOUR, 11, 11, 0, 0, 3, NOW() + INTERVAL 1 DAY, '北京市昌平区部门5路2号', '13900000004', '300M Broadband'),
(2030, 'WODEMO20260203', 2030, 8, 13, 'DISPATCHED', NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR, 13, 13, 0, 0, 3, NOW() + INTERVAL 1 DAY, '北京市朝阳区部门4路3号', '13900000003', '1G Broadband'),
(2040, 'WODEMO20260204', 2040, 9, 14, 'DISPATCHED', NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 4 HOUR, NOW() - INTERVAL 4 HOUR, 14, 14, 0, 0, 3, NOW() + INTERVAL 1 DAY, '北京市东城区部门5子队4号', '13900000001', '500M Broadband');
