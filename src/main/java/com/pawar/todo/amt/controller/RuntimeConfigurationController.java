package com.pawar.todo.amt.controller;

import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.pawar.todo.amt.response.RuntimeConfigurationResponse;
import com.pawar.todo.amt.response.RuntimeConfigurationUpdate;
import com.pawar.todo.amt.model.ApplicationConfiguration;

@RestController
@RequestMapping("/api/configuration")
public class RuntimeConfigurationController {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeConfigurationController.class);

    private final boolean healthCheckEnabled;
    private final boolean serviceManagementEnabled;
    private final boolean alertManagementEnabled;
    private final boolean backFillDataPopulationEnabled;
    private final String healthCheckCron;
    private final com.pawar.todo.amt.respository.ApplicationConfigurationRepository repository;

    public RuntimeConfigurationController(
            @Value("${healthcheck.enabled}") boolean healthCheckEnabled,
            @Value("${service-management.enabled}") boolean serviceManagementEnabled,
            @Value("${alert-management.enabled}") boolean alertManagementEnabled,
            @Value("${backfill-data.populate.enabled:false}") boolean backFillDataPopulationEnabled,
            @Value("${healthcheck.cron}") String healthCheckCron,
            com.pawar.todo.amt.respository.ApplicationConfigurationRepository repository) {
        this.healthCheckEnabled = healthCheckEnabled;
        this.serviceManagementEnabled = serviceManagementEnabled;
        this.alertManagementEnabled = alertManagementEnabled;
        this.backFillDataPopulationEnabled = backFillDataPopulationEnabled;
        this.healthCheckCron = healthCheckCron;
        this.repository = repository;
    }

    @GetMapping("/runtime")
    public RuntimeConfigurationResponse runtime() {
        return new RuntimeConfigurationResponse(value("healthcheck.enabled", healthCheckEnabled),
                value("service-management.enabled", serviceManagementEnabled),
                value("alert-management.enabled", alertManagementEnabled),
                value("backfill-data.populate.enabled", backFillDataPopulationEnabled), healthCheckCron);
    }

    @PutMapping("/runtime")
    public RuntimeConfigurationResponse update(@RequestBody RuntimeConfigurationUpdate update) {
        logger.info(
                "Updating runtime configuration: healthCheckEnabled={}, serviceManagementEnabled={}, alertManagementEnabled={}, backFillDataPopulationEnabled={}",
                update.healthCheckEnabled(), update.serviceManagementEnabled(), update.alertManagementEnabled(),
                update.backFillDataPopulationEnabled());
        if (update.healthCheckEnabled() != null) {
            save("healthcheck.enabled", update.healthCheckEnabled().toString());
        }
        if (update.serviceManagementEnabled() != null) {
            save("service-management.enabled", update.serviceManagementEnabled().toString());
        }
        if (update.alertManagementEnabled() != null) {
            save("alert-management.enabled", update.alertManagementEnabled().toString());
        }
        if (update.backFillDataPopulationEnabled() != null) {
            save("backfill-data.populate.enabled", update.backFillDataPopulationEnabled().toString());
        }
        return runtime();
    }

    private boolean value(String key, boolean fallback) {
        return repository.findById(key).map(configuration -> configuration.getValue())
                .map(Boolean::parseBoolean).orElse(fallback);
    }

    private void save(String key, String value) {
        ApplicationConfiguration configuration = repository.findById(key)
                .orElseGet(ApplicationConfiguration::new);
        configuration.setKey(key);
        configuration.setValue(value);
        repository.save(configuration);
        repository.flush();
        logger.info("Saved runtime configuration key={}", key);
    }
}
