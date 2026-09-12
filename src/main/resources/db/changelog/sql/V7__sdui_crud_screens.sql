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
