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
