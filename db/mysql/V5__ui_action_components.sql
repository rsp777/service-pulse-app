-- Typed SDUI components support the existing Service Pulse pages through a single renderer.
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ui_actions' AND column_name = 'component_type');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ui_actions ADD COLUMN component_type VARCHAR(24) NOT NULL DEFAULT ''ACTION'' AFTER action_endpoint', 'SELECT 1');
PREPARE add_component_type FROM @sql; EXECUTE add_component_type; DEALLOCATE PREPARE add_component_type;

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ui_actions' AND column_name = 'component_config');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ui_actions ADD COLUMN component_config JSON NULL AFTER component_type', 'SELECT 1');
PREPARE add_component_config FROM @sql; EXECUTE add_component_config; DEALLOCATE PREPARE add_component_config;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'ui_actions' AND index_name = 'ix_ui_actions_context_layout');
SET @sql = IF(@index_exists = 0, 'CREATE INDEX ix_ui_actions_context_layout ON ui_actions (view_context, sidebar_category, panel_title)', 'SELECT 1');
PREPARE add_context_layout_index FROM @sql; EXECUTE add_context_layout_index; DEALLOCATE PREPARE add_context_layout_index;