-- Service Pulse database migration
-- Run this script while connected to the application's service-pulse-app database.

CREATE TABLE IF NOT EXISTS application_configuration (
    configuration_key VARCHAR(100) NOT NULL,
    configuration_value VARCHAR(500) NOT NULL,
    last_updated_dttm DATETIME NULL,
    PRIMARY KEY (configuration_key)
);

CREATE TABLE IF NOT EXISTS server_service_configuration (
    configuration_id INT NOT NULL AUTO_INCREMENT,
    server_id INT NOT NULL,
    service_id INT NOT NULL,
    health_check_url VARCHAR(255) NULL,
    created_dttm DATETIME NULL,
    last_updated_dttm DATETIME NULL,
    created_source VARCHAR(255) NULL,
    last_updated_source VARCHAR(255) NULL,
    PRIMARY KEY (configuration_id),
    UNIQUE KEY uq_server_service_configuration (server_id, service_id)
);

SET @column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'service_health_status'
      AND column_name = 'server_id'
);
SET @sql = IF(
    @column_exists = 0,
    'ALTER TABLE service_health_status ADD COLUMN server_id INT NULL',
    'SELECT 1'
);
PREPARE add_server_id FROM @sql;
EXECUTE add_server_id;
DEALLOCATE PREPARE add_server_id;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'service_health_status'
      AND index_name = 'uq_service_health_server'
);
SET @sql = IF(
    @index_exists = 0,
    'ALTER TABLE service_health_status ADD UNIQUE KEY uq_service_health_server (server_id, service_id)',
    'SELECT 1'
);
PREPARE add_health_index FROM @sql;
EXECUTE add_health_index;
DEALLOCATE PREPARE add_health_index;

-- Preserve existing shared URLs as the initial value for every server-service pair.
-- Existing server-specific values are never overwritten.
SET @legacy_url_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'service'
      AND column_name = 'healthCheckUrl'
);
SET @sql = IF(
    @legacy_url_exists = 1,
    'INSERT INTO server_service_configuration (server_id, service_id, health_check_url, created_dttm, last_updated_dttm, created_source, last_updated_source) SELECT ss.server_id, ss.service_id, s.healthCheckUrl, NOW(), NOW(), ''DATABASE_MIGRATION'', ''DATABASE_MIGRATION'' FROM server_service ss JOIN service s ON s.service_id = ss.service_id WHERE s.healthCheckUrl IS NOT NULL AND NOT EXISTS (SELECT 1 FROM server_service_configuration existing WHERE existing.server_id = ss.server_id AND existing.service_id = ss.service_id)',
    'SELECT 1'
);
PREPARE migrate_service_urls FROM @sql;
EXECUTE migrate_service_urls;
DEALLOCATE PREPARE migrate_service_urls;

INSERT INTO application_configuration
    (configuration_key, configuration_value, last_updated_dttm)
VALUES
    ('healthcheck.enabled', 'true', NOW()),
    ('service-management.enabled', 'true', NOW())
ON DUPLICATE KEY UPDATE
    configuration_value = configuration_value;

SET @legacy_url_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'service'
      AND column_name = 'healthCheckUrl'
);
SET @sql = IF(
    @legacy_url_exists = 1,
    'ALTER TABLE service DROP COLUMN healthCheckUrl',
    'SELECT 1'
);
PREPARE drop_legacy_url FROM @sql;
EXECUTE drop_legacy_url;
DEALLOCATE PREPARE drop_legacy_url;