SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'service' AND column_name = 'service_type');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE service ADD COLUMN service_type VARCHAR(50) NULL', 'SELECT 1');
PREPARE add_service_type FROM @sql; EXECUTE add_service_type; DEALLOCATE PREPARE add_service_type;
