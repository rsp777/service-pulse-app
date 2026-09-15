-- V20: Add toggle action to Alert Rules table

UPDATE ui_actions 
SET component_config = JSON_SET(component_config, '$.rowActions', JSON_ARRAY(
    JSON_OBJECT('label', 'Toggle', 'type', 'action', 'endpoint', '/api/alerts/{rowId}/toggle', 'method', 'PUT'),
    JSON_OBJECT('label', 'Delete', 'type', 'delete', 'endpoint', '/api/alerts/{rowId}', 'confirm', 'Delete this alert rule?', 'className', 'danger')
))
WHERE view_context = 'alerts' AND action_label = 'Alert Rules';
