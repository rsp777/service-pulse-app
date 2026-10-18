package com.example.collector.service;

import com.example.collector.*;
import com.example.collector.factory.ProbeFactory;
import com.example.collector.model.ServiceEntity;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
public class PollingService {
    private static final Logger LOG = LoggerFactory.getLogger(PollingService.class);
    private final ServiceRepository repository;
    private final ProbeFactory probeFactory;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public PollingService(ServiceRepository repository, ProbeFactory probeFactory) {
        this.repository = repository;
        this.probeFactory = probeFactory;
    }

    public void start() {
        String dbIntervalStr = repository.getConfig("poller.interval", "60").orElse("60");
        long interval = parseInterval(dbIntervalStr);
        LOG.info("Starting PollingService: Scheduled every {} seconds", interval);
        
        scheduler.scheduleAtFixedRate(this::pollServices, 0, interval, TimeUnit.SECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void pollServices() {
        LOG.info("Initiating polling cycle...");
        try {
            List<ServiceEntity> services = repository.findAllServices();
            LOG.info("Found {} services to monitor", services.size());

            for (ServiceEntity service : services) {
                probeFactory.getProbe(service.getType()).ifPresentOrElse(
                    probe -> {
                        LOG.info("Probing service: {} using {} probe", service.getName(), service.getType());
                        ProbeResult result = probe.execute(service);
                        repository.saveMetric(service.getId(), result.getValue(), result.getStatus());
                        LOG.info("Result for {}: {} - Latency: {}ms", service.getName(), result.getStatus(), result.getValue());
                    },
                    () -> LOG.warn("No probe implementation found for type: {}", service.getType())
                );
            }
        } catch (Exception e) {
            LOG.error("Critical error during polling cycle: {}", e.getMessage());
        }
        LOG.info("Polling cycle complete.");
    }

    private long parseInterval(String interval) {
        if (interval == null) return 60;
        if (interval.endsWith("s")) {
            return Long.parseLong(interval.replace("s", ""));
        }
        try {
            return Long.parseLong(interval);
        } catch (NumberFormatException e) {
            return 60;
        }
    }
}
