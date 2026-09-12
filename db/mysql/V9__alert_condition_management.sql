-- V9: Alert Condition Types and Event Logging
-- Adds customizable condition type mappings and alert event logging capabilities

CREATE TABLE IF NOT EXISTS alert_condition_type (
    condition_type_id INT NOT NULL AUTO_INCREMENT,
    condition_type_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_dttm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (condition_type_id)
);

CREATE TABLE IF NOT EXISTS alert_condition_mapping (
    mapping_id INT NOT NULL AUTO_INCREMENT,
    condition_type_id INT NOT NULL,
    condition_key VARCHAR(100) NOT NULL,
    condition_value VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_dttm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (mapping_id),
    FOREIGN KEY (condition_type_id) REFERENCES alert_condition_type(condition_type_id),
    UNIQUE KEY uc_condition_mapping (condition_type_id, condition_key)
);

CREATE TABLE IF NOT EXISTS alert_event_log (
    log_id INT NOT NULL AUTO_INCREMENT,
    alert_id INT NOT NULL,
    server_id INT,
    service_id INT,
    trigger_condition VARCHAR(500) NOT NULL,
    alert_status VARCHAR(30) NOT NULL,
    alert_message VARCHAR(1000),
    triggered_dttm DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (log_id),
    INDEX ix_alert_event_log_alert (alert_id),
    INDEX ix_alert_event_log_triggered (triggered_dttm),
    FOREIGN KEY (alert_id) REFERENCES alert_configuration(alert_id)
);

-- Seed common alert condition types
INSERT INTO alert_condition_type (condition_type_name, description) VALUES
('SERVICE_STATUS', 'Service Status Conditions'),
('HTTP_STATUS', 'HTTP Status Code Conditions'),
('RESPONSE_TIME', 'Response Time Conditions'),
('RESOURCE_USAGE', 'Resource Usage Conditions')
ON DUPLICATE KEY UPDATE condition_type_name = condition_type_name;

-- Seed condition mappings for SERVICE_STATUS
INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'UP', 'UP', 'Service is UP' FROM alert_condition_type ct WHERE ct.condition_type_name = 'SERVICE_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'DOWN', 'DOWN', 'Service is DOWN' FROM alert_condition_type ct WHERE ct.condition_type_name = 'SERVICE_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'RESTARTING', 'RESTARTING', 'Service is RESTARTING' FROM alert_condition_type ct WHERE ct.condition_type_name = 'SERVICE_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

-- Seed condition mappings for HTTP_STATUS
INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'HTTP-200', '200', 'HTTP 200 OK' FROM alert_condition_type ct WHERE ct.condition_type_name = 'HTTP_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'HTTP-404', '404', 'HTTP 404 Not Found' FROM alert_condition_type ct WHERE ct.condition_type_name = 'HTTP_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'HTTP-500', '500', 'HTTP 500 Server Error' FROM alert_condition_type ct WHERE ct.condition_type_name = 'HTTP_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'HTTP-503', '503', 'HTTP 503 Service Unavailable' FROM alert_condition_type ct WHERE ct.condition_type_name = 'HTTP_STATUS'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

-- Seed condition mappings for RESPONSE_TIME
INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'SLOW', '>1000', 'Response time > 1000ms' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESPONSE_TIME'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'VERY_SLOW', '>5000', 'Response time > 5000ms' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESPONSE_TIME'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

-- Seed condition mappings for RESOURCE_USAGE
INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'CPU_HIGH', '>80', 'CPU usage > 80%' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESOURCE_USAGE'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'MEMORY_HIGH', '>85', 'Memory usage > 85%' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESOURCE_USAGE'
ON DUPLICATE KEY UPDATE condition_key = condition_key;

INSERT INTO alert_condition_mapping (condition_type_id, condition_key, condition_value, description) 
SELECT ct.condition_type_id, 'DISK_FULL', '>90', 'Disk usage > 90%' FROM alert_condition_type ct WHERE ct.condition_type_name = 'RESOURCE_USAGE'
ON DUPLICATE KEY UPDATE condition_key = condition_key;
