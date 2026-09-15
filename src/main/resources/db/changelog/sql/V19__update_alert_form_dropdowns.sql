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
