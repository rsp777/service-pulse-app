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
