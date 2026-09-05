-- ============================================================
-- 迭代：套餐资源管理 + 客户催单（2026-09-05）
-- 1. broadband_package 增加英文名称列 name_en
-- ============================================================

ALTER TABLE `broadband_package`
  ADD COLUMN `name_en` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'English name' AFTER `name`;

-- 存量套餐补充英文名（供列表展示，可选）
UPDATE `broadband_package` SET `name_en` = '100M Broadband' WHERE `code` = 'PKG-100M' AND `name_en` IS NULL;
UPDATE `broadband_package` SET `name_en` = '300M Broadband' WHERE `code` = 'PKG-300M' AND `name_en` IS NULL;
UPDATE `broadband_package` SET `name_en` = '1000M Broadband' WHERE `code` = 'PKG-1000M' AND `name_en` IS NULL;
