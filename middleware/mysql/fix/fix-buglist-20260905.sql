-- BBPMS buglist regression data repair (2026-09-05)
-- Safe to run repeatedly against an existing database.
USE bbpms;

-- Ensure the two reported accounts retain their intended role bindings.
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
  FROM sys_user u
  JOIN sys_role r ON r.code = 'CUSTOMER_SERVICE' AND r.deleted = 0
 WHERE u.username = 'cs1' AND u.deleted = 0;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
  FROM sys_user u
  JOIN sys_role r ON r.code = 'CUSTOMER' AND r.deleted = 0
 WHERE u.username = 'customer1' AND u.deleted = 0;

-- Customer service can cancel the CREATED/REJECTED orders it is allowed to manage.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
  JOIN sys_menu m ON m.perms = 'order:cancel' AND m.deleted = 0
 WHERE r.code = 'CUSTOMER_SERVICE' AND r.deleted = 0;

-- SUPER_ADMIN must also receive permission points introduced after the base seed.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
  FROM sys_role r
 CROSS JOIN sys_menu m
 WHERE r.code = 'SUPER_ADMIN' AND r.deleted = 0 AND m.deleted = 0;

-- Verification queries:
-- SELECT u.username, r.code FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.id
-- JOIN sys_role r ON r.id=ur.role_id WHERE u.username IN ('cs1','customer1');
-- SELECT r.code, m.perms FROM sys_role r JOIN sys_role_menu rm ON rm.role_id=r.id
-- JOIN sys_menu m ON m.id=rm.menu_id WHERE r.code='CUSTOMER_SERVICE' AND m.perms='order:cancel';
