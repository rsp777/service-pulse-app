package com.example.collector;

import com.example.collector.service.PollingService;
import com.example.collector.service.ResourcePollingService;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.Micronaut;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private final PollingService pollingService;
    private final ResourcePollingService resourcePollingService;

    public Application(PollingService pollingService, ResourcePollingService resourcePollingService) {
        this.pollingService = pollingService;
        this.resourcePollingService = resourcePollingService;
    }

    @EventListener
    public void onStartup(StartupEvent event) {
        LOG.info("Service Monitor Poller booting up via OOP Architecture...");
        
        // Start the standard availability poller
        pollingService.start();
        
        // Start the resource (CPU/RAM) poller
        resourcePollingService.start();
        
        LOG.info("All polling services started successfully.");
    }

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
