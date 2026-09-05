-- ============================================================
-- 数据治理：套餐表(broadband_package)乱码修复 + 编码统一
-- 背景：
--   1. 套餐表 3 条启用套餐的 name 为 latin1 双重编码乱码
--      （UTF-8 字节被当 latin1 再编码，如 ç•…äº«å®½å¸¦ = 畅享宽带）
--   2. 套餐表编码为连字符（PKG-100M），而订单表/前端/PackageNameMap
--      均为下划线（PKG_100M），导致字典服务(PackageNameDictService)
--      无法命中。
-- 效果：修复后 PackageNameDictService 的数据库字典自动接管套餐名
--       映射（每 5 分钟刷新），订单列表显示运营维护的权威中文名。
-- 安全：订单创建只校验编码非空、不校验套餐表存在性，改编码无副作用。
-- ============================================================

-- 1) 修复乱码 name（latin1 -> utf8mb4 双转换还原真实中文）
UPDATE broadband_package
SET name = CONVERT(CAST(CONVERT(name USING latin1) AS BINARY) USING utf8mb4)
WHERE status = 1
  AND HEX(name) LIKE '%C3A5%'        -- 含 latin1 二次编码特征字节
  AND CONVERT(CAST(CONVERT(name USING latin1) AS BINARY) USING utf8mb4) REGEXP '[一-鿿]';

-- 2) 编码统一为下划线（对齐订单表 broadband_order.package_code 与前端）
--    PKG-100M  -> PKG_100M
--    PKG-300M  -> PKG_300M
--    PKG-1000M -> PKG_1000M
UPDATE broadband_package
SET code = REPLACE(code, '-', '_')
WHERE code LIKE 'PKG-%' AND code NOT LIKE '%\_%' ESCAPE '\\';

-- 3) 校验结果（应显示修复后的中文名与下划线编码）
-- SELECT id, code, name FROM broadband_package WHERE status = 1;
