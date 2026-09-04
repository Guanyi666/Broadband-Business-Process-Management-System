-- ============================================================
-- BBPMS 数据修复脚本（问题验证与修复任务 - 任务#16）
-- 生成时间: 2026-09-04
--
-- 修复内容:
--  1) broadband_order 2010/2020/2030 安装地址乱码（历史导入 UTF-8 损坏，
--     0x3F 不可逆）→ 从种子 SQL 回填正确地址
--  2) work_order 中 order_id 关联 2010/2020/2030 的所有工单（含历史
--     雪花ID副本）地址乱码 → 同步回填
--  3) 考勤口径统一：att_attendance_record 无任何打卡记录，而
--     installer_profile.on_duty 有 3 人为 1（脏种子数据）→ 统一置 0，
--     与「on_duty 由签到/签出驱动」的后端契约保持一致
--
-- 执行方式（避免命令行编码问题，用 stdin 导入）:
--   docker exec -i bbpms-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 < fix-data-issues.sql
-- ============================================================

USE bbpms;

-- ------------------------------------------------------------
-- 1) 修复 broadband_order 乱码地址（种子原文见 05-seed-demo-data.sql）
--    id=2010 → '北京市海淀区部门3路1号'
--    id=2020 → '北京市昌平区部门5路2号'
--    id=2030 → '北京市朝阳区部门4路3号'
-- ------------------------------------------------------------
UPDATE broadband_order SET install_address = '北京市海淀区部门3路1号' WHERE id = 2010 AND deleted = 0;
UPDATE broadband_order SET install_address = '北京市昌平区部门5路2号' WHERE id = 2020 AND deleted = 0;
UPDATE broadband_order SET install_address = '北京市朝阳区部门4路3号' WHERE id = 2030 AND deleted = 0;

-- ------------------------------------------------------------
-- 2) 修复 work_order 乱码地址：所有 order_id 属于 2010/2020/2030
--    的工单（含历史雪花ID副本）统一回填
-- ------------------------------------------------------------
UPDATE work_order SET install_address = '北京市海淀区部门3路1号' WHERE order_id = 2010 AND deleted = 0;
UPDATE work_order SET install_address = '北京市昌平区部门5路2号' WHERE order_id = 2020 AND deleted = 0;
UPDATE work_order SET install_address = '北京市朝阳区部门4路3号' WHERE order_id = 2030 AND deleted = 0;

-- ------------------------------------------------------------
-- 3) 考勤口径统一：on_duty 唯一数据源为签到/签出（AttendanceServiceImpl
--    签到置 1、签出置 0）。当前无任何打卡记录，故全部置 0，
--    消除「考勤报表 0 人在岗 vs 装维档案 3 人在岗」的不一致。
-- ------------------------------------------------------------
UPDATE installer_profile SET on_duty = 0, update_time = NOW();

-- ------------------------------------------------------------
-- 校验（执行后可再手动运行确认）
-- ------------------------------------------------------------
-- SELECT id, install_address FROM broadband_order WHERE id IN (2010,2020,2030);
-- SELECT DISTINCT install_address FROM work_order WHERE order_id IN (2010,2020,2030);
-- SELECT user_id, on_duty FROM installer_profile ORDER BY user_id;
