-- =============================================================================
-- 06-resource-schema.sql — 地址与网络资源管理（ITERATION 2）
-- 区域/小区/楼栋/单元/房间 + OLT/PON/ONU 台账，及其血缘（隶属关系）
-- 全部幂等：CREATE TABLE IF NOT EXISTS + _add_col_if_missing 模式，可重复执行
-- =============================================================================

USE `bbpms`;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 1. net_region 区域（地市，如"北京市"）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `net_region` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(64)  NOT NULL                      COMMENT '区域名称',
    `code`         VARCHAR(32)  NOT NULL                      COMMENT '区域编码（唯一）',
    `sort`         INT          NOT NULL DEFAULT 0,
    `status`       TINYINT      NOT NULL DEFAULT 1            COMMENT '1=启用 0=停用',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT       DEFAULT NULL,
    `update_by`    BIGINT       DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_region_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='网络区域（地市）';

-- ----------------------------------------------------------------------------
-- 2. net_community 小区/园区
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `net_community` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `region_id`    BIGINT       NOT NULL                      COMMENT 'FK net_region.id',
    `name`         VARCHAR(128) NOT NULL                      COMMENT '小区名称',
    `address`      VARCHAR(512) DEFAULT NULL,
    `lat`          DECIMAL(10,6) DEFAULT NULL,
    `lng`          DECIMAL(10,6) DEFAULT NULL,
    `grid_code`    VARCHAR(64)  DEFAULT NULL                  COMMENT '网格编码（可空）',
    `sort`         INT          NOT NULL DEFAULT 0,
    `status`       TINYINT      NOT NULL DEFAULT 1,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT       DEFAULT NULL,
    `update_by`    BIGINT       DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_community_region` (`region_id`),
    KEY `idx_community_name`   (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小区/园区';

-- ----------------------------------------------------------------------------
-- 3. net_building 楼栋
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `net_building` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `community_id` BIGINT       NOT NULL                      COMMENT 'FK net_community.id',
    `name`         VARCHAR(64)  NOT NULL                      COMMENT '楼栋名（如 1号楼）',
    `total_floors` INT          DEFAULT NULL,
    `sort`         INT          NOT NULL DEFAULT 0,
    `status`       TINYINT      NOT NULL DEFAULT 1,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT       DEFAULT NULL,
    `update_by`    BIGINT       DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_building_community` (`community_id`),
    KEY `idx_building_name`     (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='楼栋';

-- ----------------------------------------------------------------------------
-- 4. net_unit 单元
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `net_unit` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `building_id`  BIGINT       NOT NULL                      COMMENT 'FK net_building.id',
    `name`         VARCHAR(64)  NOT NULL                      COMMENT '单元名（如 1单元）',
    `sort`         INT          NOT NULL DEFAULT 0,
    `status`       TINYINT      NOT NULL DEFAULT 1,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT       DEFAULT NULL,
    `update_by`    BIGINT       DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_unit_building` (`building_id`),
    KEY `idx_unit_name`     (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='单元';

-- ----------------------------------------------------------------------------
-- 5. net_room 房间（末端，可售资源的最小粒度）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `net_room` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `unit_id`      BIGINT       NOT NULL                      COMMENT 'FK net_unit.id',
    `room_no`      VARCHAR(32)  NOT NULL                      COMMENT '房间号（如 101）',
    `is_installed` TINYINT      NOT NULL DEFAULT 0            COMMENT '0=未安装 1=已安装（在线）',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT       DEFAULT NULL,
    `update_by`    BIGINT       DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    `version`      INT          NOT NULL DEFAULT 0              COMMENT '乐观锁（BaseDO 对齐）',
    PRIMARY KEY (`id`),
    KEY `idx_room_unit` (`unit_id`),
    KEY `idx_room_installed` (`is_installed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间（末端资源）';

-- ----------------------------------------------------------------------------
-- 6. net_olt OLT 设备台账
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `net_olt` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `name`         VARCHAR(64)  NOT NULL                      COMMENT 'OLT 名称',
    `region_id`    BIGINT       NOT NULL                      COMMENT 'FK net_region.id',
    `ip`           VARCHAR(32)  DEFAULT NULL,
    `vendor`       VARCHAR(64)  DEFAULT NULL,
    `model`        VARCHAR(64)  DEFAULT NULL,
    `status`       TINYINT      NOT NULL DEFAULT 1            COMMENT '1=在线 0=离线',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT       DEFAULT NULL,
    `update_by`    BIGINT       DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    `version`      INT          NOT NULL DEFAULT 0              COMMENT '乐观锁（BaseDO 对齐）',
    PRIMARY KEY (`id`),
    KEY `idx_olt_region` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='OLT 设备';

-- ----------------------------------------------------------------------------
-- 7. net_pon PON 板卡/口
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `net_pon` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `olt_id`       BIGINT       NOT NULL                      COMMENT 'FK net_olt.id',
    `name`         VARCHAR(64)  NOT NULL                      COMMENT 'PON 口名（如 1/1/1）',
    `total_ports`  INT          NOT NULL DEFAULT 32,
    `used_ports`   INT          NOT NULL DEFAULT 0,
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT       DEFAULT NULL,
    `update_by`    BIGINT       DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_pon_olt` (`olt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='PON 板卡/口';

-- ----------------------------------------------------------------------------
-- 8. net_onu ONU 终端（绑定到房间）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `net_onu` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `room_id`      BIGINT       DEFAULT NULL                  COMMENT 'FK net_room.id（安装后绑定）',
    `pon_id`       BIGINT       DEFAULT NULL                  COMMENT 'FK net_pon.id',
    `sn`           VARCHAR(64)  NOT NULL                      COMMENT '序列号（唯一）',
    `model`        VARCHAR(64)  DEFAULT NULL,
    `status`       VARCHAR(16)  NOT NULL DEFAULT 'IN_STOCK'   COMMENT 'IN_STOCK|INSTALLED|FAULT|RETIRED',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT       DEFAULT NULL,
    `update_by`    BIGINT       DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_onu_sn` (`sn`),
    KEY `idx_onu_room` (`room_id`),
    KEY `idx_onu_pon`  (`pon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ONU 终端';

-- ----------------------------------------------------------------------------
-- 9. broadband_order 补列（幂等）— 资源核查结果
-- ----------------------------------------------------------------------------
DELIMITER $$
DROP PROCEDURE IF EXISTS `_r_add_col_if_missing` $$
CREATE PROCEDURE `_r_add_col_if_missing`(
    IN p_table VARCHAR(64),
    IN p_col   VARCHAR(64),
    IN p_def   TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND COLUMN_NAME  = p_col
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_col, ' ', p_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL _r_add_col_if_missing('broadband_order', 'room_id',         'BIGINT DEFAULT NULL COMMENT ''FK net_room.id''');
CALL _r_add_col_if_missing('broadband_order', 'resource_status', 'VARCHAR(16) DEFAULT NULL COMMENT ''RESOURCE_OK|RESOURCE_INSUFFICIENT|NO_COVERAGE''');
CALL _r_add_col_if_missing('broadband_order', 'check_remark',    'VARCHAR(255) DEFAULT NULL COMMENT ''资源核查备注''');

-- 10. 历史表补 version（BaseDO 乐观锁对齐；新表已内建）
CALL _r_add_col_if_missing('net_region',    'version', 'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁''');
CALL _r_add_col_if_missing('net_community', 'version', 'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁''');
CALL _r_add_col_if_missing('net_building',  'version', 'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁''');
CALL _r_add_col_if_missing('net_unit',      'version', 'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁''');
CALL _r_add_col_if_missing('net_room',      'version', 'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁''');
CALL _r_add_col_if_missing('net_olt',       'version', 'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁''');
CALL _r_add_col_if_missing('net_pon',       'version', 'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁''');
CALL _r_add_col_if_missing('net_onu',       'version', 'INT NOT NULL DEFAULT 0 COMMENT ''乐观锁''');

DROP PROCEDURE _r_add_col_if_missing;

-- ----------------------------------------------------------------------------
-- 10. 演示数据（幂等）— 覆盖三种核查结果
-- ----------------------------------------------------------------------------
-- 区域
INSERT IGNORE INTO `net_region` (`id`, `name`, `code`, `sort`, `status`) VALUES
(1, '北京市', 'BJ', 1, 1);

-- 小区（可安装 / 资源不足 / 暂无覆盖 三种）
INSERT IGNORE INTO `net_community` (`id`, `region_id`, `name`, `address`, `lat`, `lng`, `grid_code`, `sort`, `status`) VALUES
(1, 1, '朝阳区演示小区',   '北京市朝阳区建国路1号',    39.929000, 116.430000, 'BJ-CY-001', 1, 1),
(2, 1, '海淀区演示小区',   '北京市海淀区中关村大街1号', 39.984000, 116.310000, 'BJ-HD-001', 2, 1),
(3, 1, '望京演示小区',     '北京市朝阳区望京街10号',   39.996200, 116.480600, 'BJ-CY-002', 3, 1);

-- 楼栋
INSERT IGNORE INTO `net_building` (`id`, `community_id`, `name`, `total_floors`, `sort`, `status`) VALUES
(1, 1, '1号楼', 18, 1, 1),
(2, 1, '2号楼', 22, 2, 1),
(3, 2, 'A栋',   12, 1, 1);

-- 单元
INSERT IGNORE INTO `net_unit` (`id`, `building_id`, `name`, `sort`, `status`) VALUES
(1, 1, '1单元', 1, 1),
(2, 1, '2单元', 2, 1),
(3, 2, '1单元', 1, 1),
(4, 3, '1单元', 1, 1);

-- 房间：1号楼1单元 101（未装）、102（已装）；1号楼2单元 201（未装）；A栋1单元 301（未装）
INSERT IGNORE INTO `net_room` (`id`, `unit_id`, `room_no`, `is_installed`) VALUES
(1, 1, '101', 0),
(2, 1, '102', 1),
(3, 2, '201', 0),
(4, 4, '301', 0);

-- OLT / PON
INSERT IGNORE INTO `net_olt` (`id`, `name`, `region_id`, `ip`, `vendor`, `model`, `status`) VALUES
(1, 'OLT-BJ-CY-01', 1, '10.1.1.1', 'Huawei', 'MA5800', 1);

INSERT IGNORE INTO `net_pon` (`id`, `olt_id`, `name`, `total_ports`, `used_ports`) VALUES
(1, 1, '1/1/1', 32, 0),
(2, 1, '1/1/2', 32, 0);

-- ONU：1 台库存、1 台绑定 102（模拟已安装在线）
INSERT IGNORE INTO `net_onu` (`id`, `room_id`, `pon_id`, `sn`, `model`, `status`) VALUES
(1, NULL, NULL, 'HBH-ONY-0001', 'HG8245H', 'IN_STOCK'),
(2, 2, 1, 'HBH-ONY-0002', 'HG8245H', 'INSTALLED');

-- 映射：房间可装性（冗余，核查用）。1/3/4 可装，2 已装（资源不足 mock）
-- 核查判定（后端实现）：房号存在且未安装 → RESOURCE_OK；房号存在但已装/资源不足 → RESOURCE_INSUFFICIENT；找不到 → NO_COVERAGE

SELECT 'Resource schema + seed applied.' AS status;