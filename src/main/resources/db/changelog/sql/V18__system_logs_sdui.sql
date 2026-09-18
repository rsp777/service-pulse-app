-- V18: Add System Logs viewer and Log Level configuration to the Settings page.

-- 1. System Logs viewer (LOGS component) – shows the in-memory ring-buffer log output.
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

-- 2. Log Level Configuration form – lets the user change log levels at runtime.
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
