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
