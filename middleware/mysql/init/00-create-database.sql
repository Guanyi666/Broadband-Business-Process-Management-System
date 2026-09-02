-- ============================================================================
-- BBPMS Database & User Initialization
-- Single database `bbpms` for the modular monolith
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `bbpms`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Application user (used by Spring Boot)
CREATE USER IF NOT EXISTS 'bbpms_app'@'%' IDENTIFIED BY 'bbpms_pwd_2026';
GRANT ALL PRIVILEGES ON `bbpms`.* TO 'bbpms_app'@'%';
FLUSH PRIVILEGES;

-- Read-only user (for analytics / reporting, optional)
CREATE USER IF NOT EXISTS 'bbpms_ro'@'%' IDENTIFIED BY 'bbpms_ro_2026';
GRANT SELECT ON `bbpms`.* TO 'bbpms_ro'@'%';
FLUSH PRIVILEGES;
