package com.pawar.todo.amt.appconfig;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.model.ApplicationConfiguration;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.model.ServerServiceConfiguration;
import com.pawar.todo.amt.model.Service;
import com.pawar.todo.amt.model.ServiceHealthStatus;
import com.pawar.todo.amt.respository.ApplicationConfigurationRepository;
import com.pawar.todo.amt.respository.ServerServiceConfigurationRepository;
import com.pawar.todo.amt.respository.ServerRepository;
import com.pawar.todo.amt.respository.ServiceHealthStatusRepository;

@Component
@Order(1)
public class ConfigurationDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationDataInitializer.class);

    private final ApplicationConfigurationRepository applicationConfigurationRepository;
    private final ServerServiceConfigurationRepository serverServiceConfigurationRepository;
    private final ServerRepository serverRepository;
    private final ServiceHealthStatusRepository serviceHealthStatusRepository;

    public ConfigurationDataInitializer(ApplicationConfigurationRepository applicationConfigurationRepository,
            ServerServiceConfigurationRepository serverServiceConfigurationRepository,
            ServerRepository serverRepository,
            ServiceHealthStatusRepository serviceHealthStatusRepository) {
        this.applicationConfigurationRepository = applicationConfigurationRepository;
        this.serverServiceConfigurationRepository = serverServiceConfigurationRepository;
        this.serverRepository = serverRepository;
        this.serviceHealthStatusRepository = serviceHealthStatusRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        logger.info("Starting configuration data initialization");
        try {
            seedFlag("healthcheck.enabled", "true");
            seedFlag("service-management.enabled", "true");
            seedFlag("alert-management.enabled", "true");
            seedFlag("health-status.populate.enabled", "false");
            
            if (isEnabled("backfill.server-service-config.enabled")) {
               backfillServerServiceConfigurations();
            } else {
                logger.info("Backfilling server-service configurations is disabled");
            }
            logger.info("Configuration data initialization completed");
        } catch (RuntimeException exception) {
            logger.error("Configuration data initialization failed", exception);
            throw exception;
        }
    }

    private void seedFlag(String key, String value) {
        if (applicationConfigurationRepository.existsById(key)) {
            return;
        }
        ApplicationConfiguration configuration = new ApplicationConfiguration();
        configuration.setKey(key);
        configuration.setValue(value);
        applicationConfigurationRepository.save(configuration);
        applicationConfigurationRepository.flush();
        logger.info("Seeded application configuration key={}", key);
    }

    private boolean isEnabled(String key) {
        return applicationConfigurationRepository.findById(key)
                .map(ApplicationConfiguration::getValue)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    private void backfillServerServiceConfigurations() {
        int created = 0;
        int associations = 0;
        List<Server> servers = serverRepository.findAll();
        for (Server server : servers) {
            for (Service service : server.getServices() == null ? Set.<Service>of() : server.getServices()) {
                associations++;
                if (serverServiceConfigurationRepository.findByServerIdAndServiceId(server.getId(), service.getId()).isPresent()) {
                    continue;
                }
                ServerServiceConfiguration configuration = new ServerServiceConfiguration();
                configuration.setServer(server);
                configuration.setService(service);
                configuration.setCreatedSource("CONFIGURATION_DATA_INITIALIZER");
                configuration.setLastUpdatedSource("CONFIGURATION_DATA_INITIALIZER");
                serverServiceConfigurationRepository.save(configuration);
                serverServiceConfigurationRepository.flush();
                created++;
            }
        }
        logger.info("Backfilled server-service configuration rows: servers={}, associations={}, created={}",
                servers.size(), associations, created);
    }

}
