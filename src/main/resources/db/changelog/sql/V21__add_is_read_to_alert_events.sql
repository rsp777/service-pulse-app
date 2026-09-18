-- V21: Add is_read to alert_events and convert Alert Rules enabled column to toggle

ALTER TABLE alert_event
ADD COLUMN is_read BOOLEAN NOT NULL DEFAULT FALSE;

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
