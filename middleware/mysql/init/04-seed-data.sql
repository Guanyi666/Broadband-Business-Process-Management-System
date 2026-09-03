-- ============================================================================
-- BBPMS Seed Data — Simplified for Modular Monolith
-- All test users password = admin123 (BCrypt hash: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2)
-- ============================================================================

USE `bbpms`;

-- ----------------------------------------------------------------------------
-- Departments (2 root)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `sys_dept` (`id`, `parent_id`, `name`, `leader`, `phone`, `path`, `sort`, `status`) VALUES
(1, 0, 'BBPMS Root',     'admin',  '13800000000', '/1/',     1, 1),
(2, 1, 'Operations Dept', 'disp1',  '13800000001', '/1/2/',   1, 1);

-- ----------------------------------------------------------------------------
-- Roles (6 standard roles)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `sys_role` (`id`, `code`, `name`, `data_scope`, `status`, `sort`, `remark`) VALUES
(1, 'SUPER_ADMIN',     '超级管理员', 1, 1, 1, 'All permissions, all data scope'),
(2, 'CUSTOMER_SERVICE','客服',       4, 1, 2, 'Self-data scope'),
(3, 'AUDITOR',         '审核员',     4, 1, 3, 'Self-data scope'),
(4, 'DISPATCHER',      '调度员',     4, 1, 4, 'Self-data scope'),
(5, 'INSTALLER',       '装维工程师', 4, 1, 5, 'Self-data scope'),
(6, 'CUSTOMER',        '客户',       4, 1, 6, 'Self-data scope');

-- ----------------------------------------------------------------------------
-- Menus (curated set covering all 6 role views)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `perms`, `icon`, `sort`, `visible`, `status`) VALUES
-- Dashboard
(1, 0, 'Dashboard', '/dashboard', 'dashboard/index', 2, 'dashboard:view', 'dashboard', 1, 1, 1),

-- Customer
(10, 0, 'Customer', '/customer', 'Layout', 1, 'customer:view', 'user', 10, 1, 1),
(11, 10, 'Customer List', 'list', 'customer/list', 2, 'customer:view', NULL, 1, 1, 1),
(12, 10, 'Customer Detail', 'detail/:id', 'customer/detail', 2, 'customer:view', NULL, 2, 0, 1),

-- Order
(20, 0, 'Order', '/order', 'Layout', 1, 'order:view', 'list', 20, 1, 1),
(21, 20, 'Order List', 'list', 'order/list', 2, 'order:view', NULL, 1, 1, 1),
(22, 20, 'Create Order', 'create', 'order/create', 2, 'order:create', NULL, 2, 1, 1),
(23, 20, 'Audit Order', 'audit', 'order/audit', 2, 'order:audit', NULL, 3, 1, 1),
(24, 20, 'Order Detail', 'detail/:id', 'order/detail', 2, 'order:view', NULL, 4, 0, 1),

-- WorkOrder
(30, 0, 'WorkOrder', '/workorder', 'Layout', 1, 'workorder:view', 'documentation', 30, 1, 1),
(31, 30, 'WorkOrder List', 'list', 'workorder/list', 2, 'workorder:view', NULL, 1, 1, 1),
(32, 30, 'Dispatch Board', 'dispatch-board', 'workorder/dispatch-board', 2, 'dispatch:manual', NULL, 2, 1, 1),
(33, 30, 'WorkOrder Detail', 'detail/:id', 'workorder/detail', 2, 'workorder:view', NULL, 3, 0, 1),

-- Installer
(40, 0, 'Installer', '/installer', 'Layout', 1, 'installer:view', 'peoples', 40, 1, 1),
(41, 40, 'Installer List', 'list', 'installer/list', 2, 'installer:view', NULL, 1, 1, 1),
(42, 40, 'Installer Map', 'map', 'installer/map', 2, 'installer:view', NULL, 2, 1, 1),

-- System (RBAC)
(50, 0, 'System', '/system', 'Layout', 1, 'system:view', 'setting', 50, 1, 1),
(51, 50, 'User', 'user', 'system/user', 2, 'system:user:view', NULL, 1, 1, 1),
(52, 50, 'Role', 'role', 'system/role', 2, 'system:role:view', NULL, 2, 1, 1),
(53, 50, 'Menu', 'menu', 'system/menu', 2, 'system:menu:view', NULL, 3, 1, 1),
(54, 50, 'Dept', 'dept', 'system/dept', 2, 'system:dept:view', NULL, 4, 1, 1),

-- Notify
(60, 0, 'Notify', '/notify', 'Layout', 1, 'notify:view', 'message', 60, 1, 1),
(61, 60, 'Template', 'template', 'notify/template', 2, 'notify:template:view', NULL, 1, 1, 1),
(62, 60, 'Record', 'record', 'notify/record', 2, 'notify:record:view', NULL, 2, 1, 1),

-- Log
(70, 0, 'Log', '/log', 'Layout', 1, 'log:view', 'document', 70, 1, 1),
(71, 70, 'Operation Log', 'operation', 'log/operation', 2, 'log:view', NULL, 1, 1, 1),
(72, 70, 'Login Log', 'login', 'log/login', 2, 'log:view', NULL, 2, 1, 1),

-- File
(80, 0, 'File', '/file', 'file/index', 2, 'file:view', 'folder', 80, 1, 1),

-- Profile
(90, 0, 'Profile', '/profile', 'profile/index', 2, NULL, 'user', 90, 0, 1);

-- Button-level perms (used by @PreAuthorize)
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `perms`, `sort`, `visible`, `status`) VALUES
(101, 21, 'Audit Button', 3, 'order:audit',    1, 1, 1),
(102, 21, 'Cancel Button', 3, 'order:cancel',   2, 1, 1),
(103, 32, 'Manual Dispatch', 3, 'dispatch:manual', 1, 1, 1),
(104, 32, 'Reassign', 3, 'dispatch:reassign', 2, 1, 1),
(105, 51, 'User Add', 3, 'system:user:add', 1, 1, 1),
(106, 51, 'User Edit', 3, 'system:user:edit', 2, 1, 1),
(107, 51, 'User Delete', 3, 'system:user:delete', 3, 1, 1),
(108, 61, 'Template Edit', 3, 'notify:template:edit', 1, 1, 1),
(109, 71, 'Export Log', 3, 'log:export', 1, 1, 1);

-- ----------------------------------------------------------------------------
-- Attendance / Leave / SLA Monitor (top-level menus)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `perms`, `icon`, `sort`, `visible`, `status`) VALUES
-- Attendance section (200-209)
(200, 0, 'Attendance', '/attendance', 'Layout', 1, 'attendance:view', 'clock', 200, 1, 1),
(201, 200, 'My Attendance', 'my', 'attendance/My', 2, 'attendance:view-self', NULL, 1, 1, 1),
(202, 200, 'Team Report', 'team', 'attendance/Report', 2, 'attendance:view-all', NULL, 2, 1, 1),
(203, 200, 'Clock In', 'clock', 'attendance/ClockIn', 2, 'attendance:clock', NULL, 3, 0, 1),

-- Leave section (210-219)
(210, 0, 'Leave', '/leave', 'Layout', 1, 'leave:view', 'todo-list', 210, 1, 1),
(211, 210, 'My Leaves', 'my', 'leave/MyApplications', 2, 'leave:view-self', NULL, 1, 1, 1),
(212, 210, 'Apply Leave', 'apply', 'leave/Apply', 2, 'leave:apply', NULL, 2, 0, 1),
(213, 210, 'Approvals', 'approvals', 'leave/Approval', 2, 'leave:approve', NULL, 3, 1, 1),
(214, 210, 'Team Calendar', 'calendar', 'leave/Calendar', 2, 'leave:view-all', NULL, 4, 1, 1),

-- SLA / work-order monitoring (220-229)
(220, 0, 'SLA Monitor', '/sla', 'Layout', 1, 'workorder:sla:view', 'warning', 220, 1, 1),
(221, 220, 'Expiring Soon', 'expiring', 'sla/Expiring', 2, 'workorder:sla:view', NULL, 1, 1, 1),
(222, 220, 'Stalled Orders', 'stalled', 'sla/Stalled', 2, 'workorder:sla:view', NULL, 2, 1, 1),
(223, 220, 'Auto-Cancelled', 'auto-cancelled', 'sla/AutoCancelled', 2, 'workorder:sla:view', NULL, 3, 1, 1);

-- H5-only menus (H5 routes use hidden top-level entries; path is just a marker)
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `type`, `perms`, `icon`, `sort`, `visible`, `status`) VALUES
(230, 0, 'H5 Attendance', '/h5/attendance', 'Layout', 1, 'attendance:clock', NULL, 230, 0, 1),
(231, 0, 'H5 Leave', '/h5/leave', 'Layout', 1, 'leave:apply', NULL, 231, 0, 1);

-- Attendance / leave / SLA button-level perms
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `perms`, `sort`, `visible`, `status`) VALUES
(204, 201, 'Clock-In Button',   3, 'attendance:clock',      1, 1, 1),
(205, 201, 'Clock-Out Button',  3, 'attendance:clock',      2, 1, 1),
(215, 211, 'Apply Leave Button', 3, 'leave:apply',           1, 1, 1),
(216, 211, 'Cancel Leave Button',3, 'leave:cancel',          2, 1, 1),
(217, 213, 'Approve L1 Button',  3, 'leave:approve-l1',      1, 1, 1),
(218, 213, 'Approve L2 Button',  3, 'leave:approve-l2',      2, 1, 1),
(219, 214, 'Cancel Others Button',3,'leave:cancel-any',      3, 1, 1),
(224, 222, 'Force Reassign Button',3,'workorder:reassign',   1, 1, 1),
(225, 222, 'Force Close Button', 3, 'workorder:force-close', 2, 1, 1),
(226, 222, 'Resume Button',      3, 'workorder:resume',      3, 1, 1),
(227, 220, 'Report Stall Button',3, 'workorder:report-stall',4, 1, 1);

-- Additional button-level perms covering every @PreAuthorize code. Inserted
-- BEFORE the SUPER_ADMIN `SELECT ... FROM sys_menu` grant so admin auto-owns them.
INSERT IGNORE INTO `sys_menu` (`id`, `parent_id`, `name`, `type`, `perms`, `sort`, `visible`, `status`) VALUES
(300, 30,  'WO Create Button',     3, 'workorder:create',        1, 1, 1),
(301, 30,  'WO View Own Button',   3, 'workorder:view-own',      2, 1, 1),
(302, 30,  'WO Accept Button',     3, 'workorder:accept',        3, 1, 1),
(303, 30,  'WO Start Button',      3, 'workorder:start',         4, 1, 1),
(304, 30,  'WO Complete Button',   3, 'workorder:complete',      5, 1, 1),
(305, 30,  'WO Transfer Button',   3, 'workorder:transfer',      6, 1, 1),
(306, 30,  'WO Cancel Button',     3, 'workorder:cancel',        7, 1, 1),
(307, 30,  'WO Update Status Btn', 3, 'workorder:update-status', 8, 1, 1),
(310, 51,  'User Assign Button',   3, 'system:user:assign',      4, 1, 1),
(311, 52,  'Role Add Button',      3, 'system:role:add',         1, 1, 1),
(312, 52,  'Role Edit Button',     3, 'system:role:edit',        2, 1, 1),
(313, 52,  'Role Delete Button',   3, 'system:role:delete',      3, 1, 1),
(314, 52,  'Role Assign Button',   3, 'system:role:assign',      4, 1, 1),
(315, 53,  'Menu Add Button',      3, 'system:menu:add',         1, 1, 1),
(316, 53,  'Menu Delete Button',   3, 'system:menu:delete',      2, 1, 1),
(317, 54,  'Dept Add Button',      3, 'system:dept:add',         1, 1, 1),
(320, 21,  'Order Update Button',  3, 'order:update',            3, 1, 1),
(321, 10,  'Customer Create Btn',  3, 'customer:create',         1, 1, 1),
(322, 10,  'Customer Sensitive',   3, 'customer:view-sensitive', 2, 1, 1),
(323, 32,  'Dispatch View Button', 3, 'dispatch:view',           3, 1, 1),
(324, 30,  'Rule Config Button',   3, 'dispatch:rule:config',    9, 1, 1),
(325, 30,  'Install Arrive Btn',   3, 'install:arrive',         10, 1, 1),
(326, 30,  'Install Info Button',  3, 'install:info',           11, 1, 1),
(327, 30,  'Install Photo Button', 3, 'install:photo',          12, 1, 1),
(328, 30,  'Install Sign Button',  3, 'install:sign',           13, 1, 1),
(329, 30,  'Install Complete Btn', 3, 'install:complete',       14, 1, 1),
(330, 30,  'Install View Button',  3, 'install:view',           15, 1, 1),
(331, 61,  'SMS Send Button',      3, 'notify:sms:send',         1, 1, 1),
(332, 61,  'WeChat Send Button',   3, 'notify:wechat:send',      2, 1, 1),
(333, 80,  'File Upload Button',   3, 'file:upload',             1, 1, 1),
(334, 80,  'File Download Button', 3, 'file:download',           2, 1, 1),
(335, 80,  'File Delete Button',   3, 'file:delete',             3, 1, 1),
(336, 53,  'Menu Edit Button',     3, 'system:menu:edit',        2, 1, 1),
(337, 54,  'Dept Edit Button',     3, 'system:dept:edit',        2, 1, 1),
(338, 54,  'Dept Delete Button',   3, 'system:dept:delete',      3, 1, 1);

-- ----------------------------------------------------------------------------
-- Role-Menu Bindings
-- ----------------------------------------------------------------------------
-- SUPER_ADMIN -> all menus
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, id FROM `sys_menu` WHERE deleted = 0;

-- CUSTOMER_SERVICE -> customer, order, profile, notify/template
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 1), (2, 10), (2, 11), (2, 12),
(2, 20), (2, 21), (2, 22), (2, 24),
(2, 60), (2, 61), (2, 62),
(2, 90);

-- AUDITOR -> order (audit), profile
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(3, 1), (3, 20), (3, 21), (3, 23), (3, 24), (3, 90);

-- DISPATCHER -> dashboard, workorder, installer, profile, attendance/leave approvals, SLA monitor
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4, 1), (4, 30), (4, 31), (4, 32), (4, 33),
(4, 40), (4, 41), (4, 42),
(4, 90),
(4, 200), (4, 201), (4, 202), (4, 204), (4, 205),
(4, 210), (4, 213), (4, 214), (4, 217), (4, 218),
(4, 220), (4, 221), (4, 222), (4, 223),
(4, 224), (4, 225), (4, 226);

-- INSTALLER -> dashboard, my workorder, my attendance, my leave
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(5, 1), (5, 30), (5, 31), (5, 33), (5, 90),
(5, 200), (5, 201), (5, 203), (5, 204), (5, 205),
(5, 210), (5, 211), (5, 212), (5, 215), (5, 216),
(5, 227),
(5, 230), (5, 231);

-- CUSTOMER -> dashboard, my orders, profile
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(6, 1), (6, 20), (6, 21), (6, 24), (6, 90);

-- INSTALLER -> work-order operations + install flow (H5)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(5, 301), (5, 302), (5, 303), (5, 304), (5, 305),
(5, 325), (5, 326), (5, 327), (5, 328), (5, 329), (5, 330);

-- DISPATCHER -> create / cancel / update-status WOs, dispatch config, order update, sensitive customer
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(4, 300), (4, 301), (4, 306), (4, 307),
(4, 323), (4, 324), (4, 320), (4, 322);

-- CUSTOMER_SERVICE -> order update, customer create + sensitive
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(2, 320), (2, 321), (2, 322);

-- ----------------------------------------------------------------------------
-- Users (all password = admin123)
-- BCrypt hash: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `dept_id`, `user_type`, `status`) VALUES
(1,  'admin',    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Super Admin', '13800000000', 1, 1, 1),
(2,  'cs1',      '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'CS Alice',    '13800000001', 2, 2, 1),
(3,  'cs2',      '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'CS Bob',      '13800000002', 2, 2, 1),
(4,  'audit1',   '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Auditor Tom',  '13800000010', 2, 3, 1),
(5,  'disp1',    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Dispatcher 1', '13800000020', 2, 4, 1),
(6,  'install1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Installer A',  '13800000030', 2, 5, 1),
(7,  'install2', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Installer B',  '13800000031', 2, 5, 1),
(8,  'install3', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Installer C',  '13800000032', 2, 5, 1),
(9,  'install4', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Installer D',  '13800000033', 2, 5, 1),
(10, 'install5', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 'Installer E',  '13800000034', 2, 5, 1);

-- User-Role bindings
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1,  1),
(2,  2), (3,  2),
(4,  3),
(5,  4),
(6,  5), (7,  5), (8,  5), (9,  5), (10, 5);

-- ----------------------------------------------------------------------------
-- Installer Profiles
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `installer_profile` (`user_id`, `skill_tags`, `service_area`, `current_lat`, `current_lng`, `on_duty`, `workload`, `level`, `score`) VALUES
(6,  '["FTTH","GPON","IPTV"]',     '["BJ-CY","BJ-HD"]', 39.929000, 116.430000, 0, 0, 3, 4.85),
(7,  '["FTTH","GPON"]',           '["BJ-CY","BJ-CP"]', 39.915000, 116.420000, 0, 1, 2, 4.60),
(8,  '["FTTH","GPON","FTTR"]',      '["BJ-HD","BJ-SY"]', 39.940000, 116.450000, 0, 0, 4, 4.92),
(9,  '["FTTH","GPON","IPTV","FTTR"]','["BJ-CY","BJ-HD","BJ-SY"]', 39.920000, 116.440000, 0, 2, 5, 4.95),
(10, '["FTTH"]',                   '["BJ-CP"]',         39.910000, 116.410000, 0, 0, 1, 4.50);

-- ----------------------------------------------------------------------------
-- Demo Customer
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `customer` (`id`, `name`, `phone`, `address`, `province`, `city`, `district`, `lat`, `lng`, `grid_code`) VALUES
(1, '张三', '13900000001', '北京市朝阳区建国路1号', '北京市', '北京市', '朝阳区', 39.929000, 116.430000, 'BJ-CY-001'),
(2, '李四', '13900000002', '北京市海淀区中关村大街1号', '北京市', '北京市', '海淀区', 39.984000, 116.310000, 'BJ-HD-001');

INSERT IGNORE INTO `customer` (`id`, `name`, `phone`, `address`, `province`, `city`, `district`, `lat`, `lng`, `grid_code`) VALUES
(3, '王芳', '13900000003', '北京市朝阳区望京街10号', '北京市', '北京市', '朝阳区', 39.996200, 116.480600, 'BJ-CY-002'),
(4, '赵伟', '13900000004', '北京市昌平区回龙观东大街8号', '北京市', '北京市', '昌平区', 40.073300, 116.336900, 'BJ-CP-001'),
(5, '陈晨', '13900000005', '北京市顺义区新顺南大街12号', '北京市', '北京市', '顺义区', 40.128900, 116.654600, 'BJ-SY-001'),
(6, '刘洋', '13900000006', '北京市海淀区学院路20号', '北京市', '北京市', '海淀区', 39.987100, 116.352500, 'BJ-HD-002');

-- ----------------------------------------------------------------------------
-- Demo Orders / Workorders
-- Covers typical packages, lifecycle states, customers, dates and progress.
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `broadband_order`
    (`id`, `order_no`, `customer_id`, `package_code`, `package_name`, `install_address`,
     `expected_install_date`, `status`, `cs_id`, `auditor_id`, `audit_time`, `audit_remark`,
     `dispatch_time`, `completed_time`, `cancelled_time`, `cancel_reason`, `create_time`, `update_time`) VALUES
(1001, 'BBDEMO20260001', 1, 'PKG_100M', '100M Broadband', '北京市朝阳区建国路1号', NOW() + INTERVAL 1 DAY, 'CREATED',       2, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 1 HOUR),
(1002, 'BBDEMO20260002', 2, 'PKG_300M', '300M Broadband', '北京市海淀区中关村大街1号', NOW() + INTERVAL 1 DAY, 'WAIT_DISPATCH', 2, 4, NOW() - INTERVAL 3 HOUR, '资料完整，审核通过', NULL, NULL, NULL, NULL, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 3 HOUR),
(1003, 'BBDEMO20260003', 3, 'PKG_500M', '500M Broadband', '北京市朝阳区望京街10号', NOW() + INTERVAL 2 DAY, 'DISPATCHED',    3, 4, NOW() - INTERVAL 1 DAY, '审核通过', NOW() - INTERVAL 6 HOUR, NULL, NULL, NULL, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 6 HOUR),
(1004, 'BBDEMO20260004', 4, 'PKG_1G',   '1G Broadband',   '北京市昌平区回龙观东大街8号', NOW() + INTERVAL 4 HOUR, 'INSTALLING', 2, 4, NOW() - INTERVAL 2 DAY, '加急安装', NOW() - INTERVAL 1 DAY, NULL, NULL, NULL, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 HOUR),
(1005, 'BBDEMO20260005', 5, 'PKG_300M', '300M Broadband', '北京市顺义区新顺南大街12号', NOW() - INTERVAL 1 DAY, 'FINISHED',    3, 4, NOW() - INTERVAL 4 DAY, '审核通过', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 DAY, NULL, NULL, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 1 DAY),
(1006, 'BBDEMO20260006', 6, 'PKG_500M', '500M Broadband', '北京市海淀区学院路20号', NOW() - INTERVAL 6 DAY, 'CLOSED',       2, 4, NOW() - INTERVAL 9 DAY, '审核通过', NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 6 DAY, NULL, NULL, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 5 DAY),
(1007, 'BBDEMO20260007', 1, 'PKG_1G',   '1G Broadband',   '北京市朝阳区建国路1号', NOW() + INTERVAL 3 DAY, 'CANCELLED',    2, NULL, NULL, NULL, NULL, NULL, NOW() - INTERVAL 2 DAY, '客户变更安装计划', NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY),
(1008, 'BBDEMO20260008', 3, 'PKG_100M', '100M Broadband', '北京市朝阳区望京街10号', NOW() + INTERVAL 1 DAY, 'AUDITED',      3, 4, NOW() - INTERVAL 30 MINUTE, '等待调度处理', NULL, NULL, NULL, NULL, NOW() - INTERVAL 2 HOUR, NOW() - INTERVAL 30 MINUTE);

INSERT IGNORE INTO `appointment`
    (`id`, `order_id`, `appointment_time`, `contact_phone`, `remark`, `confirmed`, `create_time`, `update_time`) VALUES
(1101, 1001, NOW() + INTERVAL 1 DAY, '13900000001', '工作日上门', 0, NOW() - INTERVAL 1 HOUR, NOW() - INTERVAL 1 HOUR),
(1102, 1002, NOW() + INTERVAL 1 DAY, '13900000002', '提前电话联系', 1, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 3 HOUR),
(1103, 1003, NOW() + INTERVAL 2 DAY, '13900000003', '物业已报备', 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 6 HOUR),
(1104, 1004, NOW() + INTERVAL 4 HOUR, '13900000004', '加急工单', 1, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 2 HOUR),
(1105, 1005, NOW() - INTERVAL 1 DAY, '13900000005', '已完成', 1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 1 DAY),
(1106, 1006, NOW() - INTERVAL 6 DAY, '13900000006', '已归档', 1, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 5 DAY);

INSERT IGNORE INTO `work_order`
    (`id`, `work_no`, `order_id`, `installer_id`, `dispatcher_id`, `status`, `dispatch_time`,
     `accept_time`, `start_time`, `finish_time`, `install_address`, `customer_phone`, `package_name`,
     `priority`, `expected_finish_time`, `last_active_at`, `create_time`, `update_time`) VALUES
(2002, 'WODEMO20260002', 1002, NULL, 5, 'PENDING', NULL, NULL, NULL, NULL, '北京市海淀区中关村大街1号', '13900000002', '300M Broadband', 3, NOW() + INTERVAL 1 DAY, NULL, NOW() - INTERVAL 3 HOUR, NOW() - INTERVAL 3 HOUR),
(2003, 'WODEMO20260003', 1003, 6, 5, 'DISPATCHED', NOW() - INTERVAL 6 HOUR, NULL, NULL, NULL, '北京市朝阳区望京街10号', '13900000003', '500M Broadband', 3, NOW() + INTERVAL 1 DAY, NOW() - INTERVAL 10 MINUTE, NOW() - INTERVAL 6 HOUR, NOW() - INTERVAL 10 MINUTE),
(2004, 'WODEMO20260004', 1004, 7, 5, 'IN_PROGRESS', NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 22 HOUR, NOW() - INTERVAL 2 HOUR, NULL, '北京市昌平区回龙观东大街8号', '13900000004', '1G Broadband', 1, NOW() + INTERVAL 4 HOUR, NOW() - INTERVAL 5 MINUTE, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 5 MINUTE),
(2005, 'WODEMO20260005', 1005, 8, 5, 'COMPLETED', NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 20 MINUTE, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 1 DAY, '北京市顺义区新顺南大街12号', '13900000005', '300M Broadband', 3, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 1 DAY),
(2006, 'WODEMO20260006', 1006, 9, 5, 'COMPLETED', NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 8 DAY + INTERVAL 50 MINUTE, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 6 DAY, '北京市海淀区学院路20号', '13900000006', '500M Broadband', 2, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 6 DAY);

-- Keep a few installers online so the dispatch board and map are demonstrable.
UPDATE `installer_profile` SET `on_duty` = 1 WHERE `user_id` IN (6, 7, 9);

-- ----------------------------------------------------------------------------
-- Dispatch Rule (default)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `dispatch_rule` (`id`, `name`, `weight_distance`, `weight_load`, `weight_skill`, `weight_rating`, `radius_km`, `enabled`) VALUES
(1, 'default', 40, 25, 20, 15, 30, 1);

-- ----------------------------------------------------------------------------
-- Message Templates (basic)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `message_template` (`code`, `channel`, `subject`, `content`, `enabled`) VALUES
('ORDER_CREATED_SMS',     'SMS', '订单创建成功',     '【BBPMS】您的宽带订单 ${orderNo} 已创建，请等待审核。', 1),
('ORDER_AUDITED_SMS',     'SMS', '订单审核通过',     '【BBPMS】您的订单 ${orderNo} 已审核通过，工单将尽快派发。', 1),
('ORDER_CANCELLED_SMS',    'SMS', '订单已取消',       '【BBPMS】您的订单 ${orderNo} 已取消，原因：${reason}', 1),
('WORKORDER_DISPATCH_SMS','SMS', '装维已派单',       '【BBPMS】您的订单 ${orderNo} 已派单，工单号 ${workNo}。', 1),
('WORKORDER_ACCEPT_SMS',  'SMS', '装维已接单',       '【BBPMS】装维师傅 ${installerName} 已接单，即将上门服务。', 1),
('INSTALL_COMPLETED_SMS', 'SMS', '安装完成',         '【BBPMS】您的宽带（订单${orderNo}）已安装完成，感谢您的支持！', 1),
('WORKORDER_SLA_BREACH_SMS','SMS', '工单 SLA 告警',  '【BBPMS】工单 ${workNo} 已触发 SLA 告警（${reason}），请尽快处理。', 1),
('WORKORDER_RESCHEDULE_SMS','SMS', '工单改约',       '【BBPMS】您的装维师傅已与您重新约时间，新时间：${newTime}。', 1),
('LEAVE_APPROVED_SMS',    'SMS', '请假已批准',        '【BBPMS】您的请假申请（${leaveType}）已批准，时间：${startAt} ~ ${endAt}。', 1),
('LEAVE_REJECTED_SMS',    'SMS', '请假被拒',          '【BBPMS】您的请假申请被拒绝，原因：${reason}。', 1);

-- ----------------------------------------------------------------------------
-- SLA Policies (default per business type)
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `wo_sla_policy` (`business_type`, `accept_timeout_minutes`, `progress_heartbeat_timeout_hours`, `stalled_recover_hours`, `enabled`) VALUES
('BROADBAND_INSTALL', 30, 4, 24, 1),
('REPAIR',             20, 2, 12, 1),
('RELOCATION',         60, 8, 48, 1);

-- ----------------------------------------------------------------------------
-- Example leave requests (one pending, one approved) — for demo
-- ----------------------------------------------------------------------------
INSERT IGNORE INTO `lv_leave_request`
    (`id`, `applicant_id`, `leave_type`, `start_at`, `end_at`, `total_hours`, `reason`,
     `status`, `current_level`, `required_level`, `applied_at`) VALUES
(1, 6, 'CASUAL', NOW() + INTERVAL 1 DAY, NOW() + INTERVAL 1 DAY + INTERVAL 8 HOUR,
    8.00, '个人事务', 'PENDING', 0, 1, NOW()),
(2, 7, 'SICK',   NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY + INTERVAL 4 HOUR,
    4.00, '感冒发烧', 'APPROVED', 2, 2, NOW() - INTERVAL 2 DAY);

-- Record the L1 + L2 approvals for the approved sick leave (id=2)
INSERT IGNORE INTO `lv_leave_approval_record`
    (`leave_id`, `approver_id`, `approval_level`, `action`, `comment`, `create_time`) VALUES
(2, 5, 1, 'APPROVED', '注意休息', NOW() - INTERVAL 2 DAY),
(2, 1, 2, 'APPROVED', '批准',    NOW() - INTERVAL 1 DAY);
