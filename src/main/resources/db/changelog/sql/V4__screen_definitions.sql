-- Server-driven dashboard action metadata.
CREATE TABLE IF NOT EXISTS ui_actions (
    ui_action_id INT NOT NULL AUTO_INCREMENT,
    view_context VARCHAR(64) NOT NULL,
    action_label VARCHAR(128) NOT NULL,
    action_endpoint VARCHAR(255) NOT NULL,
    request_payload JSON NULL,
    sidebar_category VARCHAR(64) NULL,
    panel_title VARCHAR(64) NULL,
    grid_span VARCHAR(16) NOT NULL DEFAULT 'span-12',
    icon_class VARCHAR(64) NULL,
    PRIMARY KEY (ui_action_id),
    INDEX ix_ui_actions_view_context (view_context)
);

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ui_actions' AND column_name = 'sidebar_category');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ui_actions ADD COLUMN sidebar_category VARCHAR(64)', 'SELECT 1');
PREPARE add_sidebar_category FROM @sql; EXECUTE add_sidebar_category; DEALLOCATE PREPARE add_sidebar_category;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ui_actions' AND column_name = 'panel_title');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ui_actions ADD COLUMN panel_title VARCHAR(64)', 'SELECT 1');
PREPARE add_panel_title FROM @sql; EXECUTE add_panel_title; DEALLOCATE PREPARE add_panel_title;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ui_actions' AND column_name = 'grid_span');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ui_actions ADD COLUMN grid_span VARCHAR(16) NOT NULL DEFAULT ''span-12''', 'SELECT 1');
PREPARE add_grid_span FROM @sql; EXECUTE add_grid_span; DEALLOCATE PREPARE add_grid_span;
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'ui_actions' AND column_name = 'icon_class');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE ui_actions ADD COLUMN icon_class VARCHAR(64)', 'SELECT 1');
PREPARE add_icon_class FROM @sql; EXECUTE add_icon_class; DEALLOCATE PREPARE add_icon_class;