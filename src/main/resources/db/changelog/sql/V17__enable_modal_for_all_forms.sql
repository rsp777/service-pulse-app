-- V17: Enable modal pop-up for all CRUD forms (Servers, Commands, Alerts, Paths, Scripts)

-- Add / Edit Server → modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Server'
)
WHERE view_context = 'servers' AND action_label = 'Add / Edit Server';

-- Add / Edit Command → modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Command'
)
WHERE view_context = 'commands' AND action_label = 'Add / Edit Command';

-- Add Alert → modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Alert Rule'
)
WHERE view_context = 'alerts' AND action_label = 'Add Alert';

-- Add / Edit Path → modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Path'
)
WHERE view_context = 'settings' AND action_label = 'Add / Edit Path';

-- Add / Edit Script → modal pop-up
UPDATE ui_actions
SET component_config = JSON_SET(component_config,
    '$.modal', TRUE,
    '$.addLabel', 'Add Script'
)
WHERE view_context = 'settings' AND action_label = 'Add / Edit Script';
