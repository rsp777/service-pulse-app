-- Source: V2__server_service_configuration.sql
-- Service Pulse database migration
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

SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'service_health_status' AND column_name = 'server_id');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE service_health_status ADD COLUMN server_id INT NULL', 'SELECT 1');
PREPARE add_server_id FROM @sql; EXECUTE add_server_id; DEALLOCATE PREPARE add_server_id;

SET @index_exists = (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'service_health_status' AND index_name = 'uq_service_health_server');
SET @sql = IF(@index_exists = 0, 'ALTER TABLE service_health_status ADD UNIQUE KEY uq_service_health_server (server_id, service_id)', 'SELECT 1');
PREPARE add_health_index FROM @sql; EXECUTE add_health_index; DEALLOCATE PREPARE add_health_index;

SET @legacy_url_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'service' AND column_name = 'healthCheckUrl');
SET @sql = IF(@legacy_url_exists = 1, 'INSERT INTO server_service_configuration (server_id, service_id, health_check_url, created_dttm, last_updated_dttm, created_source, last_updated_source) SELECT ss.server_id, ss.service_id, s.healthCheckUrl, NOW(), NOW(), ''DATABASE_MIGRATION'', ''DATABASE_MIGRATION'' FROM server_service ss JOIN service s ON s.service_id = ss.service_id WHERE s.healthCheckUrl IS NOT NULL AND NOT EXISTS (SELECT 1 FROM server_service_configuration existing WHERE existing.server_id = ss.server_id AND existing.service_id = ss.service_id)', 'SELECT 1');
PREPARE migrate_service_urls FROM @sql; EXECUTE migrate_service_urls; DEALLOCATE PREPARE migrate_service_urls;

INSERT INTO application_configuration (configuration_key, configuration_value, last_updated_dttm)
VALUES ('healthcheck.enabled', 'true', NOW()), ('service-management.enabled', 'true', NOW())
ON DUPLICATE KEY UPDATE configuration_value = configuration_value;

SET @legacy_url_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'service' AND column_name = 'healthCheckUrl');
SET @sql = IF(@legacy_url_exists = 1, 'ALTER TABLE service DROP COLUMN healthCheckUrl', 'SELECT 1');
PREPARE drop_legacy_url FROM @sql; EXECUTE drop_legacy_url; DEALLOCATE PREPARE drop_legacy_url;


-- Source: V3__alert_management.sql
-- Alert rules and triggered alert history
CREATE TABLE IF NOT EXISTS alert_configuration (
    alert_id INT NOT NULL AUTO_INCREMENT,
    alert_name VARCHAR(255) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    server_id INT NULL,
    service_id INT NULL,
    condition_type VARCHAR(30) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    condition_value VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_dttm DATETIME NULL,
    last_updated_dttm DATETIME NULL,
    PRIMARY KEY (alert_id)
);

CREATE TABLE IF NOT EXISTS alert_event (
    event_id INT NOT NULL AUTO_INCREMENT,
    alert_id INT NOT NULL,
    server_id INT NULL,
    service_id INT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    triggered_dttm DATETIME NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (event_id),
    INDEX ix_alert_event_triggered (triggered_dttm)
);

INSERT INTO application_configuration (configuration_key, configuration_value, last_updated_dttm)
VALUES ('alert-management.enabled', 'true', NOW()), ('backend.alert.configuration', 'false', NOW())
ON DUPLICATE KEY UPDATE configuration_value = configuration_value;


-- Source: V4__screen_definitions.sql
-- Server-driven dashboard action metadata.
CREATE TABLE IF NOT EXISTS ui_actions (
    ui_action_id INT NOT NULL AUTO_INCREMENT,
    view_context VARCHAR(64) NOT NULL,
    action_label VARCHAR(128) NOT NULL,
    action_endpoint VARCHAR(255) NOT NULL,
    component_type VARCHAR(24) NOT NULL DEFAULT 'ACTION',
    component_config JSON NULL,
    request_payload JSON NULL,
    sidebar_category VARCHAR(64) NULL,
    panel_title VARCHAR(64) NULL,
    grid_span VARCHAR(16) NOT NULL DEFAULT 'span-12',
    icon_class VARCHAR(64) NULL,
    PRIMARY KEY (ui_action_id),
    INDEX ix_ui_actions_view_context (view_context),
    INDEX ix_ui_actions_context_layout (view_context, sidebar_category, panel_title)
);




-- Source: V5__ui_action_components.sql
-- Typed SDUI components support the existing Service Pulse pages through a single renderer.



-- Source: V6__seed_service_pulse_sdui.sql
-- Starter metadata replaces the static dashboard, command, alert, and settings list views.
-- This file is maintained with db/mysql/V6__seed_service_pulse_sdui.sql.
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Registered Servers', '/api/servers', 'TABLE', JSON_OBJECT('endpoint', '/api/servers', 'emptyMessage', 'No servers configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'hostname', 'label', 'Server'), JSON_OBJECT('field', 'ipAddress', 'label', 'Address'), JSON_OBJECT('field', 'status', 'label', 'Status'))), 'Infrastructure', 'Servers', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Registered Servers');
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Server Controls', '/api/servers', 'FORM', JSON_OBJECT('stateOnly', TRUE, 'fields', JSON_ARRAY(JSON_OBJECT('name', 'serverId', 'label', 'Active server', 'type', 'select', 'optionsEndpoint', '/api/servers', 'optionValue', 'id', 'optionLabel', 'hostname', 'stateKey', 'serverId'))), 'Infrastructure', 'Servers', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Server Controls');
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Services', '/api/server-service-configurations/server/{serverId}', 'TABLE', JSON_OBJECT('endpoint', '/api/server-service-configurations/server/{serverId}', 'emptyMessage', 'No services configured for this server.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'serviceName', 'label', 'Service'), JSON_OBJECT('field', 'serviceType', 'label', 'Type'), JSON_OBJECT('field', 'healthCheckUrl', 'label', 'Health check'))), 'Operations', 'Service Status', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Services');
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Start All Services', '/api/manage-services/start-all-service/stream?serverId={serverId}', 'ACTION', NULL, 'Operations', 'Lifecycle', 'span-4'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Start All Services');
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Stop All Services', '/api/manage-services/stop-all-service/stream?serverId={serverId}', 'ACTION', NULL, 'Operations', 'Lifecycle', 'span-4'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Stop All Services');
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Restart All Services', '/api/manage-services/restart-all-service/stream?serverId={serverId}', 'ACTION', NULL, 'Operations', 'Lifecycle', 'span-4'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Restart All Services');
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'commands', 'Configured Commands', '/api/commands', 'TABLE', JSON_OBJECT('endpoint', '/api/commands', 'emptyMessage', 'No commands configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'name', 'label', 'Name'), JSON_OBJECT('field', 'description', 'label', 'Description'), JSON_OBJECT('field', 'status', 'label', 'Status'))), 'Operations', 'Command Registry', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'commands' AND action_label = 'Configured Commands');
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'alerts', 'Alert Rules', '/api/alerts', 'TABLE', JSON_OBJECT('endpoint', '/api/alerts', 'emptyMessage', 'No alert rules configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'name', 'label', 'Name'), JSON_OBJECT('field', 'targetType', 'label', 'Target'), JSON_OBJECT('field', 'conditionValue', 'label', 'Expected value'), JSON_OBJECT('field', 'enabled', 'label', 'Enabled'))), 'Monitoring', 'Alert Rules', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'alerts' AND action_label = 'Alert Rules');
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'settings', 'Configured Paths', '/api/paths', 'TABLE', JSON_OBJECT('endpoint', '/api/paths', 'emptyMessage', 'No paths configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'pathName', 'label', 'Name'), JSON_OBJECT('field', 'pathDescription', 'label', 'Filesystem path'))), 'Configuration', 'Paths', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'settings' AND action_label = 'Configured Paths');
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'settings', 'Configured Scripts', '/api/scripts', 'TABLE', JSON_OBJECT('endpoint', '/api/scripts', 'emptyMessage', 'No scripts configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'scriptName', 'label', 'Name'), JSON_OBJECT('field', 'scriptExtension', 'label', 'Extension'))), 'Configuration', 'Scripts', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'settings' AND action_label = 'Configured Scripts');


-- Source: V7__sdui_crud_screens.sql
-- Adds edit/delete row actions to existing tables and introduces the CRUD forms and
-- dedicated Servers/Commands/Alerts/Settings pages needed to fully replace the static UI.

-- Dashboard: run an SSH command against the selected server and stream the result to the terminal.
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'SSH Command', '/api/commands/execute', 'FORM',
       JSON_OBJECT('endpoint', '/api/commands/execute?serverId={serverId}&command={command}', 'method', 'POST', 'bodyless', TRUE, 'outputTarget', 'terminal', 'submitLabel', 'Run command',
           'fields', JSON_ARRAY(
               JSON_OBJECT('name', 'commandId', 'label', 'Configured command', 'type', 'select', 'optionsEndpoint', '/api/commands', 'optionValue', 'id', 'optionLabel', 'name', 'includeBlankOption', TRUE, 'blankLabel', 'Custom command', 'populates', JSON_OBJECT('target', 'command', 'template', '{name} {parameters}')),
               JSON_OBJECT('name', 'command', 'label', 'Command', 'type', 'textarea', 'required', TRUE))),
       'Operations', 'SSH Command', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'SSH Command');

-- Add edit/delete row actions to the existing dashboard servers table.
UPDATE ui_actions SET component_config = JSON_SET(component_config, '$.rowActions', JSON_ARRAY(
        JSON_OBJECT('label', 'Select', 'type', 'state', 'stateKey', 'serverId', 'field', 'id', 'className', 'secondary'),
        JSON_OBJECT('label', 'Edit', 'type', 'state', 'stateKey', 'editServerId', 'field', 'id', 'className', 'secondary'),
        JSON_OBJECT('label', 'Delete', 'type', 'delete', 'endpoint', '/api/servers/{rowId}', 'confirm', 'Delete this server?', 'className', 'danger')))
WHERE view_context = 'dashboard' AND action_label = 'Registered Servers';

-- Servers page: table with select/edit/delete plus the add/edit form.
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'servers', 'Registered Servers', '/api/servers', 'TABLE',
       JSON_OBJECT('endpoint', '/api/servers', 'emptyMessage', 'No servers configured.',
           'columns', JSON_ARRAY(JSON_OBJECT('field', 'hostname', 'label', 'Server'), JSON_OBJECT('field', 'ipAddress', 'label', 'Address'), JSON_OBJECT('field', 'osType', 'label', 'OS'), JSON_OBJECT('field', 'status', 'label', 'Status')),
           'rowActions', JSON_ARRAY(
               JSON_OBJECT('label', 'Select', 'type', 'state', 'stateKey', 'serverId', 'field', 'id', 'className', 'secondary'),
               JSON_OBJECT('label', 'Edit', 'type', 'state', 'stateKey', 'editServerId', 'field', 'id', 'className', 'secondary'),
               JSON_OBJECT('label', 'Delete', 'type', 'delete', 'endpoint', '/api/servers/{rowId}', 'confirm', 'Delete this server?', 'className', 'danger'))),
       'Infrastructure', 'Servers', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'servers' AND action_label = 'Registered Servers');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'servers', 'Add / Edit Server', '/api/servers', 'FORM',
       JSON_OBJECT('idStateKey', 'editServerId', 'loadEndpoint', '/api/servers/{editServerId}', 'endpoint', '/api/servers', 'method', 'POST', 'updateEndpoint', '/api/servers/{editServerId}', 'updateMethod', 'PUT', 'resetStateOnSuccess', TRUE, 'submitLabel', 'Save server',
           'defaults', JSON_OBJECT('paths', JSON_ARRAY(), 'services', JSON_ARRAY(), 'createdSource', 'UI', 'lastUpdatedSource', 'UI'),
           'fields', JSON_ARRAY(
               JSON_OBJECT('name', 'hostname', 'label', 'Hostname', 'type', 'text', 'required', TRUE),
               JSON_OBJECT('name', 'ipAddress', 'label', 'IP address', 'type', 'text', 'required', TRUE),
               JSON_OBJECT('name', 'osType', 'label', 'OS type', 'type', 'select', 'required', TRUE, 'options', JSON_ARRAY('Linux', 'Windows')),
               JSON_OBJECT('name', 'status', 'label', 'Status', 'type', 'select', 'required', TRUE, 'options', JSON_ARRAY('ONLINE', 'OFFLINE', 'UNKNOWN')))),
       'Infrastructure', 'Servers', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'servers' AND action_label = 'Add / Edit Server');

-- Commands page: add row actions to the existing table and add the create/edit form.
UPDATE ui_actions SET component_config = JSON_SET(component_config, '$.rowActions', JSON_ARRAY(
        JSON_OBJECT('label', 'Edit', 'type', 'state', 'stateKey', 'editCommandId', 'field', 'id', 'className', 'secondary'),
        JSON_OBJECT('label', 'Delete', 'type', 'delete', 'endpoint', '/api/commands/{rowId}', 'confirm', 'Delete this command?', 'className', 'danger')))
WHERE view_context = 'commands' AND action_label = 'Configured Commands';

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'commands', 'Add / Edit Command', '/api/commands', 'FORM',
       JSON_OBJECT('idStateKey', 'editCommandId', 'loadEndpoint', '/api/commands/{editCommandId}', 'endpoint', '/api/commands', 'method', 'POST', 'updateEndpoint', '/api/commands/{editCommandId}', 'updateMethod', 'PUT', 'resetStateOnSuccess', TRUE, 'submitLabel', 'Save command',
           'defaults', JSON_OBJECT('createdSource', 'UI', 'lastUpdatedSource', 'UI'),
           'fields', JSON_ARRAY(
               JSON_OBJECT('name', 'name', 'label', 'Name', 'type', 'text', 'required', TRUE),
               JSON_OBJECT('name', 'description', 'label', 'Description', 'type', 'text'),
               JSON_OBJECT('name', 'parameters', 'label', 'Parameters', 'type', 'text'),
               JSON_OBJECT('name', 'status', 'label', 'Status', 'type', 'select', 'options', JSON_ARRAY('CREATED', 'ACTIVE', 'DISABLED')),
               JSON_OBJECT('name', 'result', 'label', 'Result', 'type', 'text'))),
       'Operations', 'Command Registry', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'commands' AND action_label = 'Add / Edit Command');

-- Alerts page: delete row action, recent alert events table, and the create form.
-- Alerts have no single-record GET endpoint, so editing existing rules is not supported yet.
UPDATE ui_actions SET component_config = JSON_SET(component_config, '$.rowActions', JSON_ARRAY(
        JSON_OBJECT('label', 'Delete', 'type', 'delete', 'endpoint', '/api/alerts/{rowId}', 'confirm', 'Delete this alert rule?', 'className', 'danger')))
WHERE view_context = 'alerts' AND action_label = 'Alert Rules';

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'alerts', 'Recent Triggered Alerts', '/api/alerts/events', 'TABLE',
       JSON_OBJECT('endpoint', '/api/alerts/events', 'emptyMessage', 'No triggered alerts.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'message', 'label', 'Message'), JSON_OBJECT('field', 'status', 'label', 'Status'), JSON_OBJECT('field', 'triggeredDttm', 'label', 'Triggered'))),
       'Monitoring', 'Alert History', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'alerts' AND action_label = 'Recent Triggered Alerts');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'alerts', 'Add Alert', '/api/alerts', 'FORM',
       JSON_OBJECT('endpoint', '/api/alerts', 'method', 'POST', 'submitLabel', 'Save alert',
           'fields', JSON_ARRAY(
               JSON_OBJECT('name', 'name', 'label', 'Name', 'type', 'text', 'required', TRUE),
               JSON_OBJECT('name', 'targetType', 'label', 'Target type', 'type', 'select', 'required', TRUE, 'options', JSON_ARRAY('SERVER', 'SERVICE')),
               JSON_OBJECT('name', 'serverId', 'label', 'Server ID (if target is SERVER)', 'type', 'number'),
               JSON_OBJECT('name', 'serviceId', 'label', 'Service ID (if target is SERVICE)', 'type', 'number'),
               JSON_OBJECT('name', 'conditionType', 'label', 'Condition type', 'type', 'select', 'required', TRUE, 'options', JSON_ARRAY('STATUS', 'RESPONSE_TIME')),
               JSON_OBJECT('name', 'operator', 'label', 'Operator', 'type', 'select', 'required', TRUE, 'options', JSON_ARRAY('EQUALS', 'NOT_EQUALS')),
               JSON_OBJECT('name', 'conditionValue', 'label', 'Expected value', 'type', 'text', 'required', TRUE),
               JSON_OBJECT('name', 'enabled', 'label', 'Enabled', 'type', 'checkbox'))),
       'Monitoring', 'Alert Rules', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'alerts' AND action_label = 'Add Alert');

-- Settings page: runtime configuration form plus edit/delete row actions and forms for paths/scripts.
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'settings', 'Runtime Configuration', '/api/configuration/runtime', 'FORM',
       JSON_OBJECT('loadEndpoint', '/api/configuration/runtime', 'endpoint', '/api/configuration/runtime', 'method', 'PUT', 'submitLabel', 'Save configuration',
           'fields', JSON_ARRAY(
               JSON_OBJECT('name', 'healthCheckEnabled', 'label', 'Scheduled health checks enabled', 'type', 'checkbox'),
               JSON_OBJECT('name', 'serviceManagementEnabled', 'label', 'Remote service management enabled', 'type', 'checkbox'),
               JSON_OBJECT('name', 'alertManagementEnabled', 'label', 'Alert management enabled', 'type', 'checkbox'),
               JSON_OBJECT('name', 'backFillDataPopulationEnabled', 'label', 'Backend alert configuration enabled', 'type', 'checkbox', 'loadFrom', 'healthStatusPopulationEnabled'))),
       'Configuration', 'Runtime Configuration', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'settings' AND action_label = 'Runtime Configuration');

UPDATE ui_actions SET component_config = JSON_SET(component_config, '$.rowActions', JSON_ARRAY(
        JSON_OBJECT('label', 'Edit', 'type', 'state', 'stateKey', 'editPathId', 'field', 'id', 'className', 'secondary'),
        JSON_OBJECT('label', 'Delete', 'type', 'delete', 'endpoint', '/api/paths/{rowId}', 'confirm', 'Delete this path?', 'className', 'danger')))
WHERE view_context = 'settings' AND action_label = 'Configured Paths';

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'settings', 'Add / Edit Path', '/api/paths', 'FORM',
       JSON_OBJECT('idStateKey', 'editPathId', 'loadEndpoint', '/api/paths/{editPathId}', 'endpoint', '/api/paths', 'method', 'POST', 'updateEndpoint', '/api/paths/{editPathId}', 'updateMethod', 'PUT', 'resetStateOnSuccess', TRUE, 'submitLabel', 'Save path',
           'defaults', JSON_OBJECT('servers', JSON_ARRAY(), 'scripts', JSON_ARRAY(), 'createdSource', 'UI', 'lastUpdatedSource', 'UI'),
           'fields', JSON_ARRAY(
               JSON_OBJECT('name', 'pathName', 'label', 'Name', 'type', 'text', 'required', TRUE),
               JSON_OBJECT('name', 'pathDescription', 'label', 'Filesystem path', 'type', 'text', 'required', TRUE))),
       'Configuration', 'Paths', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'settings' AND action_label = 'Add / Edit Path');

UPDATE ui_actions SET component_config = JSON_SET(component_config, '$.rowActions', JSON_ARRAY(
        JSON_OBJECT('label', 'Edit', 'type', 'state', 'stateKey', 'editScriptId', 'field', 'id', 'className', 'secondary'),
        JSON_OBJECT('label', 'Delete', 'type', 'delete', 'endpoint', '/api/scripts/{rowId}', 'confirm', 'Delete this script?', 'className', 'danger')))
WHERE view_context = 'settings' AND action_label = 'Configured Scripts';

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'settings', 'Add / Edit Script', '/api/scripts', 'FORM',
       JSON_OBJECT('idStateKey', 'editScriptId', 'loadEndpoint', '/api/scripts/{editScriptId}', 'endpoint', '/api/scripts', 'method', 'POST', 'updateEndpoint', '/api/scripts/{editScriptId}', 'updateMethod', 'PUT', 'resetStateOnSuccess', TRUE, 'submitLabel', 'Save script',
           'defaults', JSON_OBJECT('paths', JSON_ARRAY(), 'createdSource', 'UI', 'lastUpdatedSource', 'UI'),
           'fields', JSON_ARRAY(
               JSON_OBJECT('name', 'scriptName', 'label', 'Name', 'type', 'text', 'required', TRUE),
               JSON_OBJECT('name', 'scriptExtension', 'label', 'Extension', 'type', 'text', 'required', TRUE))),
       'Configuration', 'Scripts', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'settings' AND action_label = 'Add / Edit Script');


-- Source: V8__sdui_crud_api.sql
-- V8: SDUI CRUD API enablement
-- The REST API at /api/sdui/actions provides full CRUD capability for screen definitions.
-- No schema changes are required; this migration documents the feature and is primarily
-- for tracking the deployment milestone.
--
-- Screens/menus can now be dynamically added via:
--   POST /api/sdui/actions
-- with a JSON body containing viewContext, actionLabel, actionEndpoint, componentType, and componentConfig
-- (which holds field definitions, dropdown options, and config flags).
--
-- Example: adding a new "analytics" menu with a dropdown and checkbox via REST:
--   POST /api/sdui/actions
--   {
--     "viewContext": "analytics",
--     "actionLabel": "Export Data",
--     "actionEndpoint": "/api/analytics/export",
--     "componentType": "FORM",
--     "componentConfig": {
--       "endpoint": "/api/analytics/export",
--       "method": "POST",
--       "submitLabel": "Export",
--       "fields": [
--         {
--           "name": "format",
--           "label": "Export Format",
--           "type": "select",
--           "required": true,
--           "options": ["CSV", "JSON", "Excel"],
--           "includeBlankOption": true,
--           "blankLabel": "Select format..."
--         },
--         {
--           "name": "includeArchived",
--           "label": "Include archived records",
--           "type": "checkbox"
--         }
--       ]
--     },
--     "sidebarCategory": "Reporting",
--     "panelTitle": "Export",
--     "gridSpan": "span-6"
--   }
--
-- Changes are applied immediately (cache is refreshed on every CRUD operation).
-- See SduiAdminController and UiActionController for the full API contract.

-- No schema alterations needed; ui_actions table supports arbitrary JSON configurations.


-- Source: V9__alert_condition_management.sql
-- V9: Alert Condition Types and Event Logging
-- Adds customizable condition type mappings and alert event logging capabilities

CREATE TABLE IF NOT EXISTS alert_condition_type (
    condition_type_id INT NOT NULL AUTO_INCREMENT,
    condition_type_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_dttm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (condition_type_id)
);

CREATE TABLE IF NOT EXISTS alert_condition_mapping (
    mapping_id INT NOT NULL AUTO_INCREMENT,
    condition_type_id INT NOT NULL,
    condition_key VARCHAR(100) NOT NULL,
    condition_value VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_dttm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (mapping_id),
    FOREIGN KEY (condition_type_id) REFERENCES alert_condition_type(condition_type_id),
    UNIQUE KEY uc_condition_mapping (condition_type_id, condition_key)
);

CREATE TABLE IF NOT EXISTS alert_event_log (
    log_id INT NOT NULL AUTO_INCREMENT,
    alert_id INT NOT NULL,
    server_id INT,
    service_id INT,
    trigger_condition VARCHAR(500) NOT NULL,
    alert_status VARCHAR(30) NOT NULL,
    alert_message VARCHAR(1000),
    triggered_dttm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    INDEX ix_alert_event_log_alert (alert_id),
    INDEX ix_alert_event_log_triggered (triggered_dttm),
    FOREIGN KEY (alert_id) REFERENCES alert_configuration(alert_id)
);

-- Seed common alert condition types
INSERT INTO alert_condition_type (condition_type_name, description) VALUES
('SERVICE_STATUS', 'Service Status Conditions'),
('HTTP_STATUS', 'HTTP Status Code Conditions'),
('RESPONSE_TIME', 'Response Time Conditions'),
('RESOURCE_USAGE', 'Resource Usage Conditions')
ON DUPLICATE KEY UPDATE condition_type_name = condition_type_name;

-- Seed condition mappings for SERVICE_STATUS
INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'UP', 'UP', 'Service is UP' FROM alert_condition_type ct WHERE ct.condition_type_name = 'SERVICE_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'DOWN', 'DOWN', 'Service is DOWN' FROM alert_condition_type ct WHERE ct.condition_type_name = 'SERVICE_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'RESTARTING', 'RESTARTING', 'Service is RESTARTING' FROM alert_condition_type ct WHERE ct.condition_type_name = 'SERVICE_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

-- Seed condition mappings for HTTP_STATUS
INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'HTTP-200', '200', 'HTTP 200 OK' FROM alert_condition_type ct WHERE ct.condition_type_name = 'HTTP_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'HTTP-404', '404', 'HTTP 404 Not Found' FROM alert_condition_type ct WHERE ct.condition_type_name = 'HTTP_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'HTTP-500', '500', 'HTTP 500 Server Error' FROM alert_condition_type ct WHERE ct.condition_type_name = 'HTTP_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'HTTP-503', '503', 'HTTP 503 Service Unavailable' FROM alert_condition_type ct WHERE ct.condition_type_name = 'HTTP_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

-- Seed condition mappings for RESPONSE_TIME
INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'SLOW', '>1000', 'Response time > 1000ms' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESPONSE_TIME'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'VERY_SLOW', '>5000', 'Response time > 5000ms' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESPONSE_TIME'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

-- Seed condition mappings for RESOURCE_USAGE
INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'CPU_HIGH', '>80', 'CPU usage > 80%' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESOURCE_USAGE'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'MEMORY_HIGH', '>85', 'Memory usage > 85%' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESOURCE_USAGE'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'DISK_FULL', '>90', 'Disk usage > 90%' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESOURCE_USAGE'
ON DUPLICATE KEY UPDATE condition_key = condition_key;


-- Source: V10__add_service_type.sql
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'service' AND column_name = 'service_type');
SET @sql = IF(@column_exists = 0, 'ALTER TABLE service ADD COLUMN service_type VARCHAR(50) NULL', 'SELECT 1');
PREPARE add_service_type FROM @sql; EXECUTE add_service_type; DEALLOCATE PREPARE add_service_type;


-- Source: V11__add_row_actions_to_services.sql
UPDATE ui_actions 
SET component_config = JSON_OBJECT(
    'endpoint', '/api/server-service-configurations/server/{serverId}',
    'emptyMessage', 'No services configured for this server.',
    'columns', JSON_ARRAY(
        JSON_OBJECT('field', 'serviceName', 'label', 'Service'),
        JSON_OBJECT('field', 'serviceType', 'label', 'Type'),
        JSON_OBJECT('field', 'healthCheckUrl', 'label', 'Health check')
    ),
    'rowActions', JSON_ARRAY(
        JSON_OBJECT(
            'label', 'Start',
            'type', 'action',
            'endpoint', '/api/manage-services/start-service?serverId={serverId}&serviceId={serviceId}',
            'method', 'POST'
        ),
        JSON_OBJECT(
            'label', 'Stop',
            'type', 'action',
            'endpoint', '/api/manage-services/stop-service?serverId={serverId}&serviceId={serviceId}',
            'method', 'POST'
        ),
        JSON_OBJECT(
            'label', 'Logs',
            'type', 'action',
            'endpoint', '/api/logs/stream?serverId={serverId}&serviceId={serviceId}',
            'method', 'GET'
        ),
        JSON_OBJECT(
            'label', 'Edit',
            'type', 'state',
            'stateKey', 'editServiceId',
            'field', 'serviceId'
        ),
        JSON_OBJECT(
            'label', 'Delete',
            'type', 'delete',
            'endpoint', '/api/services/{serviceId}'
        )
    )
)
WHERE view_context = 'dashboard' AND action_label = 'Services';


-- Source: V12__add_status_version_columns.sql
UPDATE ui_actions 
SET component_config = JSON_SET(
    component_config,
    '$.columns', JSON_ARRAY(
        JSON_OBJECT('field', 'serviceName', 'label', 'Service'),
        JSON_OBJECT('field', 'serviceType', 'label', 'Type'),
        JSON_OBJECT('field', 'status', 'label', 'Status', 'format', 'status'),
        JSON_OBJECT('field', 'version', 'label', 'Version', 'format', 'version'),
        JSON_OBJECT('field', 'healthCheckUrl', 'label', 'Health check')
    )
)
WHERE view_context = 'dashboard' AND action_label = 'Services';


-- Source: V14__update_services_sdui.sql
UPDATE ui_actions 
SET component_type = 'LIST',
    component_config = JSON_SET(
        component_config,
        '$.template', '<span><strong>{serviceName}</strong><small>{serviceType}</small><small>Version: <span id="version-{id}">Fetching...</span> &middot; Status: <span id="indicator-{id}" class="status">Loading...</span></small></span>'
    )
WHERE view_context = 'dashboard' AND action_label = 'Services';

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Add / Edit Service', '/api/server-service-configurations', 'FORM',
    JSON_OBJECT(
        'modal', true,
        'addLabel', 'Add Service',
        'idStateKey', 'editServiceId',
        'resetStateOnSuccess', true,
        'fields', JSON_ARRAY(
            JSON_OBJECT('name', 'id', 'type', 'hidden'),
            JSON_OBJECT('name', 'serviceName', 'label', 'Service Name', 'required', true, 'readonly', true),
            JSON_OBJECT('name', 'healthCheckUrl', 'label', 'Health Check URL', 'required', false)
        )
    ),
    'Infrastructure', 'Services', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Add / Edit Service');


-- Source: V15__fix_services_table_and_modal.sql
UPDATE ui_actions 
SET component_type = 'TABLE',
    component_config = JSON_SET(
        JSON_REMOVE(component_config, '$.template'),
        '$.rowActions[3].field', 'id'
    )
WHERE view_context = 'dashboard' AND action_label = 'Services';

UPDATE ui_actions
SET component_config = JSON_SET(
    component_config,
    '$.loadEndpoint', '/api/server-service-configurations/{editServiceId}',
    '$.updateEndpoint', '/api/server-service-configurations/{editServiceId}'
)
WHERE view_context = 'dashboard' AND action_label = 'Add / Edit Service';


-- Source: V16__update_service_modal_fields.sql
UPDATE ui_actions
SET component_config = JSON_OBJECT(
    'modal', true,
    'addLabel', 'Add Service',
    'idStateKey', 'editServiceId',
    'resetStateOnSuccess', true,
    'endpoint', '/api/services/composite',
    'updateEndpoint', '/api/services/composite/{editServiceId}',
    'loadEndpoint', '/api/server-service-configurations/{editServiceId}',
    'fields', JSON_ARRAY(
        JSON_OBJECT('name', 'id', 'type', 'hidden'),
        JSON_OBJECT('name', 'serverId', 'label', 'Server', 'type', 'select', 'optionsEndpoint', '/api/servers', 'optionValue', 'id', 'optionLabel', 'hostname', 'required', true),
        JSON_OBJECT('name', 'serviceName', 'label', 'Service Name', 'required', true),
        JSON_OBJECT('name', 'serviceType', 'label', 'Service Type', 'type', 'select', 'options', JSON_ARRAY('SPRING_BOOT', 'OTHER'), 'required', true),
        JSON_OBJECT('name', 'logsPath', 'label', 'LOGS path', 'required', false),
        JSON_OBJECT('name', 'scriptsPath', 'label', 'Scripts path', 'required', false),
        JSON_OBJECT('name', 'actuatorPort', 'label', 'Actuator port', 'type', 'number', 'required', false),
        JSON_OBJECT('name', 'contextPath', 'label', 'Context path', 'required', false)
    )
)
WHERE view_context = 'dashboard' AND action_label = 'Add / Edit Service';


-- Source: V17__enable_modal_for_all_forms.sql
-- V17: Enable modal pop-up for all CRUD forms (Servers, Commands, Alerts, Paths, Scripts)

-- Add / Edit Server â†’ modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Server'
)
WHERE view_context = 'servers' AND action_label = 'Add / Edit Server';

-- Add / Edit Command â†’ modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Command'
)
WHERE view_context = 'commands' AND action_label = 'Add / Edit Command';

-- Add Alert â†’ modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Alert Rule'
)
WHERE view_context = 'alerts' AND action_label = 'Add Alert';

-- Add / Edit Path â†’ modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Path'
)
WHERE view_context = 'settings' AND action_label = 'Add / Edit Path';

-- Add / Edit Script â†’ modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Script'
)
WHERE view_context = 'settings' AND action_label = 'Add / Edit Script';


-- Source: V18__system_logs_sdui.sql
-- V18: Add System Logs viewer and Log Level configuration to the Settings page.

-- 1. System Logs viewer (LOGS component) â€“ shows the in-memory ring-buffer log output.
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'settings', 'System Logs', '/api/system/logs', 'LOGS',
       JSON_OBJECT(
           'loadEndpoint', '/api/system/logs',
           'endpoint',     '/api/system/logs',
           'streamEndpoint', '/api/system/logs/stream',
           'lines', 200
       ),
       'Diagnostics', 'System Logs', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'settings' AND action_label = 'System Logs');

-- 2. Log Level Configuration form â€“ lets the user change log levels at runtime.
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'settings', 'Log Level Configuration', '/api/system/logs/level', 'FORM',
       JSON_OBJECT(
           'endpoint', '/api/system/logs/level',
           'method',   'PUT',
           'submitLabel', 'Apply level',
           'fields', JSON_ARRAY(
               JSON_OBJECT(
                   'name', 'logger', 'label', 'Package / Logger',
                   'type', 'select', 'required', TRUE,
                   'options', JSON_ARRAY(
                       'com.pawar.todo.amt',
                       'com.pawar.sop',
                       'org.springframework',
                       'org.hibernate'
                   )
               ),
               JSON_OBJECT(
                   'name', 'level', 'label', 'Log Level',
                   'type', 'select', 'required', TRUE,
                   'options', JSON_ARRAY('TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF')
               )
           )
       ),
       'Diagnostics', 'Log Level', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'settings' AND action_label = 'Log Level Configuration');


-- Source: V19__update_alert_form_dropdowns.sql
-- V19: Update Add Alert form to use dropdowns for serverId and serviceId with conditional visibility

UPDATE ui_actions
SET component_config = JSON_OBJECT(
    'endpoint', '/api/alerts',
    'method', 'POST',
    'submitLabel', 'Save alert',
    'modal', TRUE,
    'addLabel', 'Add Alert Rule',
    'fields', JSON_ARRAY(
        JSON_OBJECT('name', 'name', 'label', 'Name', 'type', 'text', 'required', TRUE),
        JSON_OBJECT('name', 'targetType', 'label', 'Target type', 'type', 'select', 'required', TRUE, 'options', JSON_ARRAY('SERVER', 'SERVICE')),
        JSON_OBJECT(
            'name', 'serverId',
            'label', 'Server',
            'type', 'select',
            'optionsEndpoint', '/api/servers',
            'optionValue', 'id',
            'optionLabel', 'hostname',
            'visibleWhen', JSON_OBJECT('field', 'targetType', 'value', 'SERVER')
        ),
        JSON_OBJECT(
            'name', 'serviceId',
            'label', 'Service',
            'type', 'select',
            'optionsEndpoint', '/api/services',
            'optionValue', 'id',
            'optionLabel', 'serviceName',
            'visibleWhen', JSON_OBJECT('field', 'targetType', 'value', 'SERVICE')
        ),
        JSON_OBJECT('name', 'conditionType', 'label', 'Condition type', 'type', 'select', 'required', TRUE, 'options', JSON_ARRAY('STATUS', 'RESPONSE_TIME')),
        JSON_OBJECT('name', 'operator', 'label', 'Operator', 'type', 'select', 'required', TRUE, 'options', JSON_ARRAY('EQUALS', 'NOT_EQUALS')),
        JSON_OBJECT('name', 'conditionValue', 'label', 'Expected value', 'type', 'text', 'required', TRUE),
        JSON_OBJECT('name', 'enabled', 'label', 'Enabled', 'type', 'checkbox')
    )
)
WHERE view_context = 'alerts' AND action_label = 'Add Alert';


-- Source: V20__add_alert_toggle_action.sql
-- V20: Add toggle action to Alert Rules table

UPDATE ui_actions 
SET component_config = JSON_SET(component_config, '$.rowActions', JSON_ARRAY(
    JSON_OBJECT('label', 'Toggle', 'type', 'action', 'endpoint', '/api/alerts/{rowId}/toggle', 'method', 'PUT'),
    JSON_OBJECT('label', 'Delete', 'type', 'delete', 'endpoint', '/api/alerts/{rowId}', 'confirm', 'Delete this alert rule?', 'className', 'danger')
))
WHERE view_context = 'alerts' AND action_label = 'Alert Rules';


-- Source: V21__add_is_read_to_alert_events.sql
-- V21: Add is_read to alert_events and convert Alert Rules enabled column to toggle



-- Update Alert Rules table configuration to use toggle format and remove row action
UPDATE ui_actions 
SET component_config = JSON_SET(
    JSON_SET(component_config, '$.rowActions', JSON_ARRAY(
        JSON_OBJECT('label', 'Delete', 'type', 'delete', 'endpoint', '/api/alerts/{id}', 'confirm', 'Delete this alert rule?', 'className', 'danger')
    )),
    '$.columns', JSON_ARRAY(
        JSON_OBJECT('field', 'name', 'label', 'Name'), 
        JSON_OBJECT('field', 'targetType', 'label', 'Target'), 
        JSON_OBJECT('field', 'conditionValue', 'label', 'Expected value'), 
        JSON_OBJECT('field', 'enabled', 'label', 'Enabled', 'format', 'toggle')
    )
)
WHERE view_context = 'alerts' AND action_label = 'Alert Rules';


