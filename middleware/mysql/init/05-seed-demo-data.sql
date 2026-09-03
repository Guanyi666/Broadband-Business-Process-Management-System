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
