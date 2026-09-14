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
