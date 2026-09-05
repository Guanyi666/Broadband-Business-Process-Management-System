USE bbpms;

-- ============================================================================
-- BBPMS customer self-service portal
-- Account binding, package catalogue, after-sales tickets, evaluation,
-- appointment audit and profile-change approval.
-- ============================================================================

CREATE TABLE IF NOT EXISTS `customer_user_binding` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL,
    `customer_id` BIGINT       NOT NULL,
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=ACTIVE,0=DISABLED',
    `bind_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT       DEFAULT NULL,
    `update_by`   BIGINT       DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    `version`     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cub_user` (`user_id`),
    UNIQUE KEY `uk_cub_customer` (`customer_id`),
    KEY `idx_cub_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Customer portal account binding';

CREATE TABLE IF NOT EXISTS `broadband_package` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT,
    `code`        VARCHAR(64)    NOT NULL,
    `name`        VARCHAR(128)   NOT NULL,
    `speed_mbps`  INT            NOT NULL,
    `monthly_fee` DECIMAL(10, 2) NOT NULL,
    `description` VARCHAR(512)   DEFAULT NULL,
    `status`      TINYINT        NOT NULL DEFAULT 1,
    `sort`        INT            NOT NULL DEFAULT 0,
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT         DEFAULT NULL,
    `update_by`   BIGINT         DEFAULT NULL,
    `deleted`     TINYINT        NOT NULL DEFAULT 0,
    `version`     INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_package_code` (`code`),
    KEY `idx_package_status_sort` (`status`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Broadband package catalogue';

CREATE TABLE IF NOT EXISTS `customer_service_ticket` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `ticket_no`     VARCHAR(32)  NOT NULL,
    `customer_id`   BIGINT       NOT NULL,
    `order_id`      BIGINT       DEFAULT NULL,
    `work_order_id` BIGINT       DEFAULT NULL,
    `type`          VARCHAR(16)  NOT NULL COMMENT 'REPAIR/COMPLAINT',
    `category`      VARCHAR(64)  DEFAULT NULL,
    `priority`      TINYINT      NOT NULL DEFAULT 3,
    `description`   VARCHAR(1000) NOT NULL,
    `attachments`   TEXT         DEFAULT NULL COMMENT 'JSON URL array',
    `status`        VARCHAR(32)  NOT NULL DEFAULT 'SUBMITTED',
    `handler_id`    BIGINT       DEFAULT NULL,
    `handle_result` VARCHAR(1000) DEFAULT NULL,
    `accepted_time` DATETIME     DEFAULT NULL,
    `resolved_time` DATETIME     DEFAULT NULL,
    `closed_time`   DATETIME     DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`     BIGINT       DEFAULT NULL,
    `update_by`     BIGINT       DEFAULT NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `version`       INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_no` (`ticket_no`),
    KEY `idx_ticket_customer` (`customer_id`, `create_time`),
    KEY `idx_ticket_status` (`status`, `type`),
    KEY `idx_ticket_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Customer repair and complaint ticket';

CREATE TABLE IF NOT EXISTS `service_evaluation` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`          BIGINT       NOT NULL,
    `work_order_id`     BIGINT       NOT NULL,
    `customer_id`       BIGINT       NOT NULL,
    `installer_id`      BIGINT       NOT NULL,
    `overall_score`     TINYINT      NOT NULL,
    `service_score`     TINYINT      DEFAULT NULL,
    `quality_score`     TINYINT      DEFAULT NULL,
    `punctuality_score` TINYINT      DEFAULT NULL,
    `tags`              VARCHAR(512) DEFAULT NULL COMMENT 'JSON string array',
    `content`           VARCHAR(1000) DEFAULT NULL,
    `status`            TINYINT      NOT NULL DEFAULT 1,
    `create_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`         BIGINT       DEFAULT NULL,
    `update_by`         BIGINT       DEFAULT NULL,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    `version`           INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_evaluation_order` (`order_id`),
    KEY `idx_evaluation_installer` (`installer_id`, `status`),
    KEY `idx_evaluation_customer` (`customer_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Customer service evaluation';

CREATE TABLE IF NOT EXISTS `customer_profile_change` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `customer_id`      BIGINT        NOT NULL,
    `applicant_user_id` BIGINT       NOT NULL,
    `change_fields`    VARCHAR(255)  NOT NULL,
    `proposed_name`    VARCHAR(256)  DEFAULT NULL COMMENT 'SM4 ciphertext',
    `proposed_phone`   VARCHAR(256)  DEFAULT NULL COMMENT 'SM4 ciphertext',
    `proposed_id_card_no` VARCHAR(256) DEFAULT NULL COMMENT 'SM4 ciphertext',
    `proposed_address` VARCHAR(512)  DEFAULT NULL,
    `status`           VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    `reviewer_id`      BIGINT        DEFAULT NULL,
    `review_remark`    VARCHAR(512)  DEFAULT NULL,
    `review_time`      DATETIME      DEFAULT NULL,
    `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`        BIGINT        DEFAULT NULL,
    `update_by`        BIGINT        DEFAULT NULL,
    `deleted`          TINYINT       NOT NULL DEFAULT 0,
    `version`          INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_profile_change_customer` (`customer_id`, `create_time`),
    KEY `idx_profile_change_status` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Customer profile change approval';

CREATE TABLE IF NOT EXISTS `appointment_change_log` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT,
    `appointment_id`       BIGINT       NOT NULL,
    `order_id`             BIGINT       NOT NULL,
    `old_appointment_time` DATETIME     DEFAULT NULL,
    `new_appointment_time` DATETIME     NOT NULL,
    `operator_id`          BIGINT       NOT NULL,
    `operator_role`        VARCHAR(32)  NOT NULL,
    `reason`               VARCHAR(512) DEFAULT NULL,
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`            BIGINT       DEFAULT NULL,
    `update_by`            BIGINT       DEFAULT NULL,
    `deleted`              TINYINT      NOT NULL DEFAULT 0,
    `version`              INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_appointment_change_order` (`order_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Appointment reschedule audit';

-- Backward-compatible columns for existing tables.
DROP PROCEDURE IF EXISTS `_cp_add_col_if_missing`;
DELIMITER $$
CREATE PROCEDURE `_cp_add_col_if_missing`(
    IN p_table VARCHAR(64), IN p_col VARCHAR(64), IN p_def TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_col
    ) THEN
        SET @cp_sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_col, '` ', p_def);
        PREPARE cp_stmt FROM @cp_sql;
        EXECUTE cp_stmt;
        DEALLOCATE PREPARE cp_stmt;
    END IF;
END $$
DELIMITER ;

CALL _cp_add_col_if_missing('broadband_order', 'source', 'VARCHAR(32) NOT NULL DEFAULT ''CS''');
CALL _cp_add_col_if_missing('appointment', 'status', 'VARCHAR(16) NOT NULL DEFAULT ''PENDING''');
CALL _cp_add_col_if_missing('appointment', 'reschedule_count', 'INT NOT NULL DEFAULT 0');
CALL _cp_add_col_if_missing('appointment', 'confirmed_by', 'BIGINT DEFAULT NULL');
CALL _cp_add_col_if_missing('appointment', 'confirmed_time', 'DATETIME DEFAULT NULL');
CALL _cp_add_col_if_missing('work_order', 'business_type', 'VARCHAR(32) NOT NULL DEFAULT ''BROADBAND_INSTALL''');

DROP PROCEDURE IF EXISTS `_cp_add_col_if_missing`;

-- Customer-portal permissions are hidden permission points, not admin pages.
INSERT IGNORE INTO `sys_menu`
(`id`, `parent_id`, `name`, `type`, `perms`, `sort`, `visible`, `status`) VALUES
(500, 0, 'Portal Profile View',       3, 'customer-portal:profile:view',       1, 0, 1),
(501, 0, 'Portal Profile Update',     3, 'customer-portal:profile:update',     2, 0, 1),
(502, 0, 'Portal Order View',         3, 'customer-portal:order:view',         3, 0, 1),
(503, 0, 'Portal Order Create',       3, 'customer-portal:order:create',       4, 0, 1),
(504, 0, 'Portal Appointment Update', 3, 'customer-portal:appointment:update', 5, 0, 1),
(505, 0, 'Portal Ticket Create',      3, 'customer-portal:ticket:create',      6, 0, 1),
(506, 0, 'Portal Ticket View',        3, 'customer-portal:ticket:view',        7, 0, 1),
(507, 0, 'Portal Evaluation Create',  3, 'customer-portal:evaluation:create',  8, 0, 1),
(508, 0, 'Portal Message View',       3, 'customer-portal:message:view',       9, 0, 1),
(509, 0, 'Portal Password Change',    3, 'customer-portal:password:update',   10, 0, 1),
(510, 0, 'Portal Admin',              3, 'customer-portal:admin',             11, 0, 1);

-- Replace the legacy generic-order grants. Customer data ownership is not
-- create_by SELF scope, so it must be enforced by dedicated portal endpoints.
DELETE FROM `sys_role_menu`
WHERE `role_id` = 6 AND `menu_id` IN (1, 20, 21, 24, 90);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(6, 500), (6, 501), (6, 502), (6, 503), (6, 504),
(6, 505), (6, 506), (6, 507), (6, 508), (6, 509),
(1, 500), (1, 501), (1, 502), (1, 503), (1, 504),
(1, 505), (1, 506), (1, 507), (1, 508), (1, 509), (1, 510),
(2, 510);

INSERT IGNORE INTO `broadband_package`
(`id`, `code`, `name`, `speed_mbps`, `monthly_fee`, `description`, `status`, `sort`) VALUES
(1, 'PKG-100M',  '畅享宽带 100M',  100,  69.00, '适合日常上网和高清视频', 1, 1),
(2, 'PKG-300M',  '家庭宽带 300M',  300,  99.00, '适合多人家庭和在线学习', 1, 2),
(3, 'PKG-1000M', '千兆宽带 1000M', 1000, 159.00, '适合游戏、直播和智能家庭', 1, 3);

-- Demo customer account: customer1 / admin123, bound to demo customer id=1.
INSERT IGNORE INTO `sys_user`
(`username`, `password`, `nickname`, `real_name`, `phone`, `user_type`, `status`) VALUES
('customer1', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
 '张三', '张三', '13900000001', 6, 1);

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT `id`, 6 FROM `sys_user` WHERE `username` = 'customer1' AND `user_type` = 6;

INSERT IGNORE INTO `customer_user_binding`
(`user_id`, `customer_id`, `status`, `create_by`, `update_by`)
SELECT `id`, 1, 1, 1, 1 FROM `sys_user` WHERE `username` = 'customer1' AND `user_type` = 6;
