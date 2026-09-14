UPDATE ui_actions 
SET component_type = 'LIST',
    component_config = JSON_SET(
        component_config,
        '$.template', '<span><strong>{serviceName}</strong><small>{serviceType}</small><small>Version: <span id="version-{id}">Fetching...</span> &middot; Status: <span id="indicator-{id}" class="status">Loading...</span></small></span>'
    )
WHERE view_context = 'dashboard' AND action_label = 'Services';

INSERT INTO ui_actions (view_context, action_label, action_endpoint, component_type, component_config, sidebar_category, panel_title, grid_span)
SELECT 'dashboard', 'Add / Edit Service', '/api/server-service-configurations', 'FORM',
    JSON_OBJECT(
        'modal', true,
        'addLabel', 'Add Service',
        'idStateKey', 'editServiceId',
        'resetStateOnSuccess', true,
        'fields', JSON_ARRAY(
            JSON_OBJECT('name', 'id', 'type', 'hidden'),
            JSON_OBJECT('name', 'serviceName', 'label', 'Service Name', 'required', true, 'readonly', true),
            JSON_OBJECT('name', 'healthCheckUrl', 'label', 'Health Check URL', 'required', false)
        )
    ),
    'Infrastructure', 'Services', 'span-12'
WHERE NOT EXISTS (SELECT 1 FROM ui_actions WHERE view_context = 'dashboard' AND action_label = 'Add / Edit Service');
