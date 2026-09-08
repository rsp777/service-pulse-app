-- Alert rules and triggered alert history
CREATE TABLE IF NOT EXISTS alert_configuration (
    alert_id INT NOT NULL AUTO_INCREMENT,
    alert_name VARCHAR(255) NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    server_id INT NULL,
    service_id INT NULL,
    condition_type VARCHAR(30) NOT NULL,
    operator VARCHAR(30) NOT NULL,
    condition_value VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_dttm DATETIME NULL,
    last_updated_dttm DATETIME NULL,
    PRIMARY KEY (alert_id)
);

CREATE TABLE IF NOT EXISTS alert_event (
    event_id INT NOT NULL AUTO_INCREMENT,
    alert_id INT NOT NULL,
    server_id INT NULL,
    service_id INT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    triggered_dttm DATETIME NOT NULL,
    PRIMARY KEY (event_id),
    INDEX ix_alert_event_triggered (triggered_dttm)
);

INSERT INTO application_configuration (configuration_key, configuration_value, last_updated_dttm)
VALUES ('alert-management.enabled', 'true', NOW())
ON DUPLICATE KEY UPDATE configuration_value = configuration_value;

INSERT INTO application_configuration (configuration_key, configuration_value, last_updated_dttm)
VALUES ('backend.alert.configuration', 'false', NOW())
ON DUPLICATE KEY UPDATE configuration_value = configuration_value;
