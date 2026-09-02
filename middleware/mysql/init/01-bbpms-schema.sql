-- ============================================================================
-- BBPMS Merged Schema — Modular Monolith
-- All business tables in one database (no undo_log, no outbox_event)
-- ============================================================================

USE `bbpms`;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- sys_dept
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_dept` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT          COMMENT 'Primary key',
    `parent_id`    BIGINT       NOT NULL DEFAULT 0               COMMENT 'Parent dept id (0 = root)',
    `name`         VARCHAR(64)  NOT NULL                         COMMENT 'Department name',
    `leader`       VARCHAR(64)  DEFAULT NULL                     COMMENT 'Department leader name',
    `phone`        VARCHAR(32)  DEFAULT NULL                     COMMENT 'Contact phone',
    `path`         VARCHAR(500) NOT NULL DEFAULT '/'             COMMENT 'Materialized path',
    `sort`         INT          NOT NULL DEFAULT 0               COMMENT 'Display sort order',
    `status`       TINYINT      NOT NULL DEFAULT 1               COMMENT '1=enabled, 0=disabled',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT       DEFAULT NULL,
    `update_by`    BIGINT       DEFAULT NULL,
    `deleted`      TINYINT      NOT NULL DEFAULT 0,
    `version`      INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_status`    (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System department';

-- ----------------------------------------------------------------------------
-- sys_user
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `username`        VARCHAR(64)  NOT NULL,
    `password`        VARCHAR(128) NOT NULL,
    `salt`            VARCHAR(32)  DEFAULT NULL,
    `nickname`        VARCHAR(64)  DEFAULT NULL,
    `avatar`          VARCHAR(512) DEFAULT NULL,
    `phone`           VARCHAR(32)  DEFAULT NULL,
    `email`           VARCHAR(128) DEFAULT NULL,
    `dept_id`         BIGINT       DEFAULT NULL,
    `user_type`       TINYINT      NOT NULL DEFAULT 1  COMMENT '1=SUPER_ADMIN,2=CS,3=AUDITOR,4=DISPATCHER,5=INSTALLER,6=CUSTOMER',
    `status`          TINYINT      NOT NULL DEFAULT 1,
    `last_login_ip`   VARCHAR(64)  DEFAULT NULL,
    `last_login_time` DATETIME     DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       BIGINT       DEFAULT NULL,
    `update_by`       BIGINT       DEFAULT NULL,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `version`         INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone`    (`phone`),
    UNIQUE KEY `uk_email`    (`email`),
    KEY `idx_dept_status` (`dept_id`, `status`),
    KEY `idx_user_type`   (`user_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System user';

-- ----------------------------------------------------------------------------
-- sys_role
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `code`        VARCHAR(64)  NOT NULL,
    `name`        VARCHAR(64)  NOT NULL,
    `data_scope`  TINYINT      NOT NULL DEFAULT 1 COMMENT '1=ALL,2=DEPT,3=DEPT_AND_CHILD,4=SELF,5=CUSTOM',
    `status`      TINYINT      NOT NULL DEFAULT 1,
    `sort`        INT          NOT NULL DEFAULT 0,
    `remark`      VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT       DEFAULT NULL,
    `update_by`   BIGINT       DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    `version`     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System role';

-- ----------------------------------------------------------------------------
-- sys_menu
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_menu` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `parent_id`  BIGINT       NOT NULL DEFAULT 0,
    `name`       VARCHAR(64)  NOT NULL,
    `path`       VARCHAR(255) DEFAULT NULL,
    `component`  VARCHAR(255) DEFAULT NULL,
    `type`       TINYINT      NOT NULL DEFAULT 2 COMMENT '1=DIR,2=MENU,3=BUTTON',
    `perms`      VARCHAR(100) DEFAULT NULL,
    `icon`       VARCHAR(64)  DEFAULT NULL,
    `sort`       INT          NOT NULL DEFAULT 0,
    `visible`    TINYINT      NOT NULL DEFAULT 1,
    `status`     TINYINT      NOT NULL DEFAULT 1,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`  BIGINT       DEFAULT NULL,
    `update_by`  BIGINT       DEFAULT NULL,
    `deleted`    TINYINT      NOT NULL DEFAULT 0,
    `version`    INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_perms`     (`perms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='System menu';

-- ----------------------------------------------------------------------------
-- sys_user_role
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `user_id` BIGINT NOT NULL,
    `role_id` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- sys_role_menu
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
    `role_id` BIGINT NOT NULL,
    `menu_id` BIGINT NOT NULL,
    PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- installer_profile
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `installer_profile` (
    `user_id`           BIGINT         NOT NULL,
    `id_card_no`        VARCHAR(64)    DEFAULT NULL,
    `skill_tags`         JSON           DEFAULT NULL,
    `service_area`      JSON           DEFAULT NULL,
    `current_lat`       DECIMAL(10, 6) DEFAULT NULL,
    `current_lng`       DECIMAL(10, 6) DEFAULT NULL,
    `last_location_time` DATETIME      DEFAULT NULL,
    `on_duty`           TINYINT        NOT NULL DEFAULT 0,
    `workload`          INT            NOT NULL DEFAULT 0,
    `level`             TINYINT        NOT NULL DEFAULT 1,
    `score`             DECIMAL(3, 2)  NOT NULL DEFAULT 5.00,
    `create_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`         BIGINT         DEFAULT NULL,
    `update_by`         BIGINT         DEFAULT NULL,
    `deleted`           TINYINT        NOT NULL DEFAULT 0,
    `version`           INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Installer profile';

-- ----------------------------------------------------------------------------
-- customer
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `customer` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `name`       VARCHAR(64)  NOT NULL,
    `id_card_no` VARCHAR(64)  DEFAULT NULL,
    `phone`      VARCHAR(32)  DEFAULT NULL,
    `address`    VARCHAR(512) DEFAULT NULL,
    `province`   VARCHAR(32)  DEFAULT NULL,
    `city`       VARCHAR(32)  DEFAULT NULL,
    `district`   VARCHAR(32)  DEFAULT NULL,
    `lat`        DECIMAL(10, 6) DEFAULT NULL,
    `lng`        DECIMAL(10, 6) DEFAULT NULL,
    `grid_code`  VARCHAR(32)  DEFAULT NULL,
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`  BIGINT      DEFAULT NULL,
    `update_by`  BIGINT      DEFAULT NULL,
    `deleted`    TINYINT     NOT NULL DEFAULT 0,
    `version`    INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_phone`      (`phone`),
    KEY `idx_id_card_no` (`id_card_no`),
    KEY `idx_grid`       (`grid_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Customer';

-- ----------------------------------------------------------------------------
-- broadband_order
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `broadband_order` (
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT,
    `order_no`             VARCHAR(32)  NOT NULL,
    `customer_id`          BIGINT       NOT NULL,
    `package_code`         VARCHAR(64)  NOT NULL,
    `package_name`         VARCHAR(128) DEFAULT NULL,
    `install_address`      VARCHAR(512) NOT NULL,
    `expected_install_date` DATETIME    DEFAULT NULL,
    `status`               VARCHAR(32)  NOT NULL DEFAULT 'CREATED',
    `cs_id`                BIGINT       DEFAULT NULL,
    `auditor_id`           BIGINT       DEFAULT NULL,
    `audit_time`           DATETIME     DEFAULT NULL,
    `audit_remark`         VARCHAR(512) DEFAULT NULL,
    `dispatch_time`        DATETIME     DEFAULT NULL,
    `completed_time`       DATETIME     DEFAULT NULL,
    `cancelled_time`       DATETIME     DEFAULT NULL,
    `cancel_reason`        VARCHAR(512) DEFAULT NULL,
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`            BIGINT       DEFAULT NULL,
    `update_by`            BIGINT       DEFAULT NULL,
    `deleted`              TINYINT      NOT NULL DEFAULT 0,
    `version`              INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no`     (`order_no`),
    KEY `idx_customer_id`       (`customer_id`),
    KEY `idx_status_create_time` (`status`, `create_time`),
    KEY `idx_auditor_id`        (`auditor_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Broadband order';

-- ----------------------------------------------------------------------------
-- appointment
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `appointment` (
    `id`               BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`         BIGINT       NOT NULL,
    `appointment_time` DATETIME     NOT NULL,
    `contact_phone`    VARCHAR(32)  DEFAULT NULL,
    `remark`           VARCHAR(255) DEFAULT NULL,
    `confirmed`        TINYINT      NOT NULL DEFAULT 0,
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`        BIGINT       DEFAULT NULL,
    `update_by`        BIGINT       DEFAULT NULL,
    `deleted`          TINYINT      NOT NULL DEFAULT 0,
    `version`          INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Appointment';

-- ----------------------------------------------------------------------------
-- order_audit_log
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `order_audit_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `order_id`    BIGINT       NOT NULL,
    `auditor_id`  BIGINT       DEFAULT NULL,
    `from_status` VARCHAR(32)  DEFAULT NULL,
    `to_status`   VARCHAR(32)  DEFAULT NULL,
    `remark`      VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT       DEFAULT NULL,
    `update_by`   BIGINT       DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    `version`     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order_create_time` (`order_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Order audit log';

-- ----------------------------------------------------------------------------
-- work_order
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `work_no`         VARCHAR(32)  NOT NULL,
    `order_id`        BIGINT       NOT NULL,
    `installer_id`    BIGINT       DEFAULT NULL,
    `dispatcher_id`   BIGINT       DEFAULT NULL,
    `status`          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    `dispatch_time`   DATETIME     DEFAULT NULL,
    `accept_time`     DATETIME     DEFAULT NULL,
    `start_time`      DATETIME     DEFAULT NULL,
    `finish_time`     DATETIME     DEFAULT NULL,
    `install_address` VARCHAR(512) DEFAULT NULL,
    `customer_phone`  VARCHAR(32)  DEFAULT NULL,
    `package_name`    VARCHAR(128) DEFAULT NULL,
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       BIGINT       DEFAULT NULL,
    `update_by`       BIGINT       DEFAULT NULL,
    `deleted`         TINYINT      NOT NULL DEFAULT 0,
    `version`         INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_no`           (`work_no`),
    KEY `idx_installer_status`        (`installer_id`, `status`),
    KEY `idx_status`                  (`status`),
    KEY `idx_order_id`                (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Work order';

-- ----------------------------------------------------------------------------
-- work_order_timeline
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `work_order_timeline` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `work_order_id` BIGINT       NOT NULL,
    `from_status`   VARCHAR(32)  DEFAULT NULL,
    `to_status`     VARCHAR(32)  DEFAULT NULL,
    `operator_id`   BIGINT       DEFAULT NULL,
    `operator_role` VARCHAR(32)  DEFAULT NULL,
    `remark`        VARCHAR(512) DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`     BIGINT       DEFAULT NULL,
    `update_by`     BIGINT       DEFAULT NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `version`       INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_work_order_create_time` (`work_order_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Work order timeline';

-- ----------------------------------------------------------------------------
-- dispatch_record
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dispatch_record` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `work_order_id`   BIGINT       DEFAULT NULL,
    `installer_id`    BIGINT       DEFAULT NULL,
    `strategy`        VARCHAR(32)  NOT NULL DEFAULT 'AUTO' COMMENT 'AUTO/MANUAL/REASSIGN',
    `score`           DECIMAL(5, 2) DEFAULT NULL,
    `candidates_json` TEXT          DEFAULT NULL,
    `reason`          TEXT          DEFAULT NULL,
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       BIGINT        DEFAULT NULL,
    `update_by`       BIGINT        DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0,
    `version`         INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_work_order_id` (`work_order_id`),
    KEY `idx_installer_id`  (`installer_id`),
    KEY `idx_strategy`      (`strategy`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dispatch record';

-- ----------------------------------------------------------------------------
-- dispatch_rule
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dispatch_rule` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT,
    `name`            VARCHAR(64) NOT NULL DEFAULT 'default',
    `weight_distance` INT NOT NULL DEFAULT 40,
    `weight_load`     INT NOT NULL DEFAULT 25,
    `weight_skill`    INT NOT NULL DEFAULT 20,
    `weight_rating`   INT NOT NULL DEFAULT 15,
    `radius_km`       INT NOT NULL DEFAULT 30,
    `enabled`         TINYINT NOT NULL DEFAULT 1,
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       BIGINT DEFAULT NULL,
    `update_by`       BIGINT DEFAULT NULL,
    `deleted`         TINYINT NOT NULL DEFAULT 0,
    `version`         INT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dispatch rule';

-- ----------------------------------------------------------------------------
-- install_record
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `install_record` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `work_order_id`         BIGINT       NOT NULL,
    `installer_id`          BIGINT       DEFAULT NULL,
    `onu_mac`               VARCHAR(32)  DEFAULT NULL,
    `onu_sn`                VARCHAR(64)  DEFAULT NULL,
    `olt_port`              VARCHAR(32)  DEFAULT NULL,
    `signal_db`             DECIMAL(5, 2) DEFAULT NULL,
    `start_lat`             DECIMAL(10, 6) DEFAULT NULL,
    `start_lng`             DECIMAL(10, 6) DEFAULT NULL,
    `complete_lat`          DECIMAL(10, 6) DEFAULT NULL,
    `complete_lng`          DECIMAL(10, 6) DEFAULT NULL,
    `photos`                JSON          DEFAULT NULL,
    `signature_url`         VARCHAR(512)  DEFAULT NULL,
    `customer_signature_name` VARCHAR(64) DEFAULT NULL,
    `remark`                TEXT          DEFAULT NULL,
    `submit_time`           DATETIME      DEFAULT NULL,
    `status`                VARCHAR(32)   NOT NULL DEFAULT 'PENDING',
    `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`             BIGINT        DEFAULT NULL,
    `update_by`             BIGINT        DEFAULT NULL,
    `deleted`               TINYINT       NOT NULL DEFAULT 0,
    `version`               INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_work_order_id` (`work_order_id`),
    KEY `idx_installer_id`   (`installer_id`),
    KEY `idx_status`         (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Install record';

-- ----------------------------------------------------------------------------
-- attachment
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `attachment` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `object_key`    VARCHAR(256) NOT NULL,
    `bucket`        VARCHAR(64)  DEFAULT NULL,
    `original_name` VARCHAR(255) DEFAULT NULL,
    `content_type`  VARCHAR(128) DEFAULT NULL,
    `size`          BIGINT       DEFAULT NULL,
    `biz_type`      VARCHAR(32)  DEFAULT NULL COMMENT 'ORDER/INSTALL/CUSTOMER/OTHER',
    `biz_id`        BIGINT       DEFAULT NULL,
    `uploader_id`   BIGINT       DEFAULT NULL,
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`     BIGINT       DEFAULT NULL,
    `update_by`     BIGINT       DEFAULT NULL,
    `deleted`       TINYINT      NOT NULL DEFAULT 0,
    `version`       INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_object_key` (`object_key`),
    KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Attachment';

-- ----------------------------------------------------------------------------
-- message
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       DEFAULT NULL,
    `channel`      VARCHAR(16)  NOT NULL DEFAULT 'SMS' COMMENT 'SMS/WECHAT/INAPP',
    `template_code` VARCHAR(64) DEFAULT NULL,
    `params`       JSON          DEFAULT NULL,
    `content`      TEXT          DEFAULT NULL,
    `status`       VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
    `error_msg`    TEXT          DEFAULT NULL,
    `send_time`    DATETIME      DEFAULT NULL,
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT        DEFAULT NULL,
    `update_by`    BIGINT        DEFAULT NULL,
    `deleted`      TINYINT       NOT NULL DEFAULT 0,
    `version`      INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_create` (`user_id`, `create_time`),
    KEY `idx_status`      (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Message';

-- ----------------------------------------------------------------------------
-- message_template
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `message_template` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT,
    `code`                VARCHAR(64)  NOT NULL,
    `channel`             VARCHAR(16)  NOT NULL DEFAULT 'SMS',
    `subject`             VARCHAR(255) DEFAULT NULL,
    `content`             TEXT          NOT NULL,
    `aliyun_template_id`   VARCHAR(64)   DEFAULT NULL,
    `wechat_template_id`  VARCHAR(64)   DEFAULT NULL,
    `enabled`              TINYINT      NOT NULL DEFAULT 1,
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`            BIGINT       DEFAULT NULL,
    `update_by`            BIGINT       DEFAULT NULL,
    `deleted`              TINYINT      NOT NULL DEFAULT 0,
    `version`              INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Message template';

-- ----------------------------------------------------------------------------
-- operation_log
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       DEFAULT NULL,
    `module`       VARCHAR(64)  DEFAULT NULL,
    `action`       VARCHAR(255) DEFAULT NULL,
    `request_uri`  VARCHAR(255) DEFAULT NULL,
    `method`       VARCHAR(8)   DEFAULT NULL,
    `params`       TEXT          DEFAULT NULL,
    `result`       TEXT          DEFAULT NULL,
    `ip`           VARCHAR(64)   DEFAULT NULL,
    `user_agent`   VARCHAR(512)  DEFAULT NULL,
    `cost_ms`      INT           DEFAULT NULL,
    `status`       TINYINT       NOT NULL DEFAULT 1,
    `error`        TEXT          DEFAULT NULL,
    `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`    BIGINT        DEFAULT NULL,
    `update_by`    BIGINT        DEFAULT NULL,
    `deleted`      TINYINT       NOT NULL DEFAULT 0,
    `version`      INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`),
    KEY `idx_module`    (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Operation log';

-- ----------------------------------------------------------------------------
-- login_log
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `login_log` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       DEFAULT NULL,
    `username`    VARCHAR(64)  DEFAULT NULL,
    `ip`          VARCHAR(64)  DEFAULT NULL,
    `user_agent`  VARCHAR(512) DEFAULT NULL,
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1=SUCCESS,0=FAIL',
    `message`     VARCHAR(255) DEFAULT NULL,
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`   BIGINT       DEFAULT NULL,
    `update_by`   BIGINT       DEFAULT NULL,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    `version`     INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Login log';

-- ----------------------------------------------------------------------------
-- idempotency_record
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `idempotency_record` (
    `idempotency_key` VARCHAR(64) NOT NULL,
    `biz_type`         VARCHAR(64) DEFAULT NULL,
    `biz_id`           BIGINT      DEFAULT NULL,
    `result_json`      MEDIUMTEXT  DEFAULT NULL,
    `expire_time`      DATETIME    DEFAULT NULL,
    `create_time`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`idempotency_key`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Idempotency record';
