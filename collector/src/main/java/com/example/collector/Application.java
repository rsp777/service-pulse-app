package com.example.collector;

import io.micronaut.context.annotation.Value;
import io.micronaut.runtime.Micronaut;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Singleton
public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);
    private final ServiceRepository repository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Probe> probeMap = new HashMap<>();

    @Value("${poller.interval:60s}")
    private String pollIntervalStr;

    public Application(ServiceRepository repository) {
        this.repository = repository;
        // Initialize probes
        probeMap.put("http", new HttpProbe());
        probeMap.put("tcp", new TcpProbe());
        probeMap.put("ssh", new SshProbe());
    }

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }

    @EventListener
    public void onStartup(StartupEvent event) {
        LOG.info("Service Monitor Poller starting up...");
        
        long interval = parseInterval(pollIntervalStr);
        LOG.info("Scheduled polling every {} seconds", interval);

        scheduler.scheduleAtFixedRate(this::pollServices, 0, interval, TimeUnit.SECONDS);
    }

    private void pollServices() {
        LOG.info("Starting polling cycle...");
        try {
            List<ServiceEntity> services = repository.findAllServices();
            LOG.info("Found {} services to monitor", services.size());

            for (ServiceEntity service : services) {
                Probe probe = probeMap.get(service.getType().toLowerCase());
                if (probe == null) {
                    LOG.warn("No probe implementation found for type: {}", service.getType());
                    continue;
                }

                LOG.info("Probing service: {} ({}) using {} probe", service.getName(), service.getUrl(), service.getType());
                ProbeResult result = probe.execute(service);
                
                repository.saveMetric(service.getId(), result.getValue(), result.getStatus());
                LOG.info("Result for {}: {} - Latency: {}ms", service.getName(), result.getStatus(), result.getValue());
            }
        } catch (Exception e) {
            LOG.error("Critical error during polling cycle: {}", e.getMessage());
        }
        LOG.info("Polling cycle complete.");
    }

    private long parseInterval(String interval) {
        if (interval.endsWith("s")) {
            return Long.parseLong(interval.replace("s", ""));
        }
        return 60; // default
    }
}


