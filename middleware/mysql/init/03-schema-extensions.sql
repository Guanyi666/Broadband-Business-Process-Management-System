SET NAMES utf8mb4;
-- =============================================================================
-- BBPMS Schema Extensions — Phases 3-6 (Attendance, Leave, SLA + missing cols)
-- Canonical extension file (merged from the two earlier 03 files).
-- Runs after 01-bbpms-schema.sql on first init.
-- Adds: attendance, leave, work-order SLA tables; work_order / sys_user /
-- operation_log missing columns. All operations are idempotent so the
-- script is safe to re-run.
-- =============================================================================

USE `bbpms`;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- =============================================================================
-- 1. att_attendance_record  — daily clock-in / clock-out per installer
-- =============================================================================
CREATE TABLE IF NOT EXISTS `att_attendance_record` (
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `installer_id`        BIGINT       NOT NULL                COMMENT 'FK sys_user.id',
    `work_date`           DATE         NOT NULL                COMMENT 'The calendar day this record covers',
    `clock_in_at`         DATETIME     DEFAULT NULL            COMMENT 'Sign-in timestamp (nullable until first sign-in)',
    `clock_out_at`        DATETIME     DEFAULT NULL            COMMENT 'Sign-out timestamp',
    `work_minutes`        INT          DEFAULT NULL            COMMENT 'Computed (clock_out_at - clock_in_at) / 60s; null until clock-out',
    `break_minutes`       INT          NOT NULL DEFAULT 0     COMMENT 'Total break time accumulated today',
    `status`              VARCHAR(16)  NOT NULL DEFAULT 'OFF_DUTY'
                                                              COMMENT 'OFF_DUTY | ON_DUTY | ON_BREAK | AUTO_OFF',
    `source`              VARCHAR(16)  NOT NULL DEFAULT 'MANUAL'
                                                              COMMENT 'MANUAL | GPS | SCHEDULED | HEARTBEAT_TIMEOUT',
    `location_lat`        DECIMAL(10,6) DEFAULT NULL           COMMENT 'GPS lat at sign-in',
    `location_lng`        DECIMAL(10,6) DEFAULT NULL           COMMENT 'GPS lng at sign-in',
    `remark`              VARCHAR(255) DEFAULT NULL            COMMENT 'Optional note',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`           BIGINT       DEFAULT NULL,
    `update_by`           BIGINT       DEFAULT NULL,
    `deleted`             TINYINT      NOT NULL DEFAULT 0,
    `version`             INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_installer_date` (`installer_id`, `work_date`),
    KEY `idx_status`             (`status`),
    KEY `idx_clock_in_at`        (`clock_in_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Daily attendance record';

-- =============================================================================
-- 2. att_attendance_summary  — monthly rollup for reports
-- =============================================================================
CREATE TABLE IF NOT EXISTS `att_attendance_summary` (
    `id`                       BIGINT       NOT NULL AUTO_INCREMENT,
    `installer_id`             BIGINT       NOT NULL,
    `year_month`               CHAR(7)      NOT NULL                COMMENT 'YYYY-MM',
    `total_work_minutes`       INT          NOT NULL DEFAULT 0,
    `work_days`                INT          NOT NULL DEFAULT 0,
    `late_count`               INT          NOT NULL DEFAULT 0,
    `early_leave_count`        INT          NOT NULL DEFAULT 0,
    `absent_count`             INT          NOT NULL DEFAULT 0,
    `last_calculated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`                BIGINT       DEFAULT NULL,
    `update_by`                BIGINT       DEFAULT NULL,
    `deleted`                  TINYINT      NOT NULL DEFAULT 0,
    `version`                  INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_installer_month` (`installer_id`, `year_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Monthly attendance rollup';

-- =============================================================================
-- 3. lv_leave_request  — leave application with multi-level approval
-- =============================================================================
CREATE TABLE IF NOT EXISTS `lv_leave_request` (
    `id`                   BIGINT        NOT NULL AUTO_INCREMENT,
    `applicant_id`         BIGINT        NOT NULL                COMMENT 'FK sys_user.id (the installer)',
    `leave_type`           VARCHAR(16)   NOT NULL                COMMENT 'CASUAL | ANNUAL | SICK | COMPASSIONATE | UNPAID',
    `start_at`             DATETIME      NOT NULL,
    `end_at`               DATETIME      NOT NULL,
    `total_hours`          DECIMAL(6,2)  NOT NULL DEFAULT 0     COMMENT 'Computed (end_at - start_at) hours',
    `reason`               VARCHAR(500)  NOT NULL,
    `attachment_url`       VARCHAR(255)  DEFAULT NULL            COMMENT 'Optional, e.g. medical certificate for SICK',
    `status`               VARCHAR(16)   NOT NULL DEFAULT 'PENDING'
                                                              COMMENT 'PENDING | APPROVED | REJECTED | CANCELLED',
    `current_level`        TINYINT       NOT NULL DEFAULT 0     COMMENT 'How many levels have been approved so far',
    `required_level`       TINYINT       NOT NULL DEFAULT 1     COMMENT '1 or 2; decided at apply time by the escalation rule',
    `level1_approver_id`   BIGINT        DEFAULT NULL,
    `level1_decided_at`    DATETIME      DEFAULT NULL,
    `level1_remark`        VARCHAR(255)  DEFAULT NULL,
    `level2_approver_id`   BIGINT        DEFAULT NULL,
    `level2_decided_at`    DATETIME      DEFAULT NULL,
    `level2_remark`        VARCHAR(255)  DEFAULT NULL,
    `applied_at`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `create_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`            BIGINT        DEFAULT NULL,
    `update_by`            BIGINT        DEFAULT NULL,
    `deleted`              TINYINT       NOT NULL DEFAULT 0,
    `version`              INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_applicant_status` (`applicant_id`, `status`),
    KEY `idx_status_start`     (`status`, `start_at`),
    KEY `idx_leave_type`       (`leave_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Leave application with multi-level approval';

-- =============================================================================
-- 4. lv_leave_approval_record  — audit trail for each approval/rejection step
-- =============================================================================
CREATE TABLE IF NOT EXISTS `lv_leave_approval_record` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT,
    `leave_id`        BIGINT        NOT NULL,
    `approver_id`     BIGINT        NOT NULL,
    `approval_level`  TINYINT       NOT NULL                COMMENT '1 or 2',
    `action`          VARCHAR(16)   NOT NULL                COMMENT 'APPROVED | REJECTED | ESCALATED',
    `comment`         VARCHAR(500)  DEFAULT NULL,
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`       BIGINT        DEFAULT NULL,
    `update_by`       BIGINT        DEFAULT NULL,
    `deleted`         TINYINT       NOT NULL DEFAULT 0,
    `version`         INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_leave_id`     (`leave_id`),
    KEY `idx_approver`     (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit trail for leave approval steps';

-- =============================================================================
-- 5. wo_sla_policy  — per-business-type SLA thresholds
-- =============================================================================
CREATE TABLE IF NOT EXISTS `wo_sla_policy` (
    `id`                              INT       NOT NULL AUTO_INCREMENT,
    `business_type`                   VARCHAR(32)  NOT NULL              COMMENT 'BROADBAND_INSTALL | REPAIR | RELOCATION',
    `accept_timeout_minutes`          INT          NOT NULL DEFAULT 30  COMMENT 'DISPATCHED → not accepted within → AUTO_CANCELLED',
    `progress_heartbeat_timeout_hours` INT         NOT NULL DEFAULT 4   COMMENT 'IN_PROGRESS → no heartbeat within → STALLED',
    `stalled_recover_hours`           INT          NOT NULL DEFAULT 24  COMMENT 'STALLED → not resumed within → AUTO_CANCELLED',
    `enabled`                         TINYINT      NOT NULL DEFAULT 1,
    `create_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`                     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `create_by`                       BIGINT       DEFAULT NULL,
    `update_by`                       BIGINT       DEFAULT NULL,
    `deleted`                         TINYINT      NOT NULL DEFAULT 0,
    `version`                         INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_type` (`business_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SLA policy per business type';

-- =============================================================================
-- 6. Missing columns (idempotent ALTERs via information_schema check)
-- =============================================================================
-- The stored-procedure pattern keeps the script idempotent on re-run.

DELIMITER $$
DROP PROCEDURE IF EXISTS `_add_col_if_missing` $$
CREATE PROCEDURE `_add_col_if_missing`(
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

-- work_order: SLA / lifecycle columns
CALL _add_col_if_missing('work_order', 'priority',           'TINYINT      NOT NULL DEFAULT 3');
CALL _add_col_if_missing('work_order', 'expected_finish_time','DATETIME    DEFAULT NULL');
CALL _add_col_if_missing('work_order', 'last_active_at',     'DATETIME    DEFAULT NULL');
CALL _add_col_if_missing('work_order', 'stall_reason',       'VARCHAR(255) DEFAULT NULL');
CALL _add_col_if_missing('work_order', 'cancel_type',        'VARCHAR(32)  DEFAULT NULL');

-- sys_user: fields required by the SysUser entity
CALL _add_col_if_missing('sys_user', 'real_name',       'VARCHAR(64)   DEFAULT NULL');
CALL _add_col_if_missing('sys_user', 'phone_enc',       'VARCHAR(256)  DEFAULT NULL');
CALL _add_col_if_missing('sys_user', 'id_card_no_enc',  'VARCHAR(256)  DEFAULT NULL');
CALL _add_col_if_missing('sys_user', 'gender',          'TINYINT       DEFAULT NULL');
CALL _add_col_if_missing('sys_user', 'birthday',        'DATE          DEFAULT NULL');
CALL _add_col_if_missing('sys_user', 'tenant_id',       'BIGINT        DEFAULT NULL');

-- operation_log: username field used by the entity
CALL _add_col_if_missing('operation_log', 'username',   'VARCHAR(64)   DEFAULT NULL');

-- customer: status column used by the Customer entity / CustomerMapper.xml
CALL _add_col_if_missing('customer', 'status',   'TINYINT       NOT NULL DEFAULT 0');

-- attendance: break-start tracking (SA-P2-003 — real elapsed break duration)
CALL _add_col_if_missing('att_attendance_record', 'break_start_at', 'DATETIME DEFAULT NULL');

-- Add indexes via the same idempotent pattern.
DROP PROCEDURE IF EXISTS `_add_idx_if_missing`;
DELIMITER $$
CREATE PROCEDURE `_add_idx_if_missing`(
    IN p_table VARCHAR(64),
    IN p_idx   VARCHAR(64),
    IN p_def   TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = p_table
          AND INDEX_NAME   = p_idx
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX ', p_idx, ' ', p_def);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL _add_idx_if_missing('work_order', 'idx_status_last_active', '(status, last_active_at)');

DROP PROCEDURE _add_col_if_missing;
DROP PROCEDURE _add_idx_if_missing;

-- Done.
SELECT 'Schema extensions applied.' AS status;
