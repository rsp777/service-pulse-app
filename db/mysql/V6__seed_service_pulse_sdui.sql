-- Starter metadata replaces the static dashboard, command, alert, and settings list views.
-- Administrators can add or change rows without redeploying the application.
INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Registered Servers', '/api/servers', 'TABLE',
       JSON_OBJECT('endpoint', '/api/servers', 'emptyMessage', 'No servers configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'hostname', 'label', 'Server'), JSON_OBJECT('field', 'ipAddress', 'label', 'Address'), JSON_OBJECT('field', 'status', 'label', 'Status'))),
       'Infrastructure', 'Servers', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Registered Servers');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Server Controls', '/api/servers', 'FORM',
    JSON_OBJECT('stateOnly', TRUE, 'fields', JSON_ARRAY(JSON_OBJECT('name', 'serverId', 'label', 'Active server', 'type', 'select', 'optionsEndpoint', '/api/servers', 'optionValue', 'id', 'optionLabel', 'hostname', 'stateKey', 'serverId'))),
       'Infrastructure', 'Servers', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Server Controls');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Services', '/api/server-service-configurations/server/{serverId}', 'TABLE',
       JSON_OBJECT('endpoint', '/api/server-service-configurations/server/{serverId}', 'emptyMessage', 'No services configured for this server.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'serviceName', 'label', 'Service'), JSON_OBJECT('field', 'serviceType', 'label', 'Type'), JSON_OBJECT('field', 'healthCheckUrl', 'label', 'Health check'))),
       'Operations', 'Service Status', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Services');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Start All Services', '/api/manage-services/start-all-service/stream?serverId={serverId}', 'ACTION', NULL,
       'Operations', 'Lifecycle', 'span-4'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Start All Services');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Stop All Services', '/api/manage-services/stop-all-service/stream?serverId={serverId}', 'ACTION', NULL,
       'Operations', 'Lifecycle', 'span-4'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Stop All Services');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Restart All Services', '/api/manage-services/restart-all-service/stream?serverId={serverId}', 'ACTION', NULL,
       'Operations', 'Lifecycle', 'span-4'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Restart All Services');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'commands', 'Configured Commands', '/api/commands', 'TABLE',
       JSON_OBJECT('endpoint', '/api/commands', 'emptyMessage', 'No commands configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'name', 'label', 'Name'), JSON_OBJECT('field', 'description', 'label', 'Description'), JSON_OBJECT('field', 'status', 'label', 'Status'))),
       'Operations', 'Command Registry', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'commands' AND action_label = 'Configured Commands');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'alerts', 'Alert Rules', '/api/alerts', 'TABLE',
       JSON_OBJECT('endpoint', '/api/alerts', 'emptyMessage', 'No alert rules configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'name', 'label', 'Name'), JSON_OBJECT('field', 'targetType', 'label', 'Target'), JSON_OBJECT('field', 'conditionValue', 'label', 'Expected value'), JSON_OBJECT('field', 'enabled', 'label', 'Enabled'))),
       'Monitoring', 'Alert Rules', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'alerts' AND action_label = 'Alert Rules');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'settings', 'Configured Paths', '/api/paths', 'TABLE',
       JSON_OBJECT('endpoint', '/api/paths', 'emptyMessage', 'No paths configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'pathName', 'label', 'Name'), JSON_OBJECT('field', 'pathDescription', 'label', 'Filesystem path'))),
       'Configuration', 'Paths', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'settings' AND action_label = 'Configured Paths');

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'settings', 'Configured Scripts', '/api/scripts', 'TABLE',
       JSON_OBJECT('endpoint', '/api/scripts', 'emptyMessage', 'No scripts configured.', 'columns', JSON_ARRAY(JSON_OBJECT('field', 'scriptName', 'label', 'Name'), JSON_OBJECT('field', 'scriptExtension', 'label', 'Extension'))),
       'Configuration', 'Scripts', 'span-6'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'settings' AND action_label = 'Configured Scripts');