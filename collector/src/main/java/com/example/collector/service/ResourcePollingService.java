package com.example.collector.service;

import com.example.collector.*;
import com.example.collector.model.ServiceEntity;
import com.example.collector.model.ResourceMetric;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.jcraft.jsch.*;
import java.util.Properties;

@Singleton
public class ResourcePollingService {
    private static final Logger LOG = LoggerFactory.getLogger(ResourcePollingService.class);
    private final ServiceRepository repository;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ResourcePollingService(ServiceRepository repository) {
        this.repository = repository;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::pollResources, 0, 60, TimeUnit.SECONDS);
    }

    private void pollResources() {
        LOG.info("Initiating resource polling cycle...");
        try {
            List<ServiceEntity> services = repository.findAllServices();
            for (ServiceEntity service : services) {
                ResourceMetric metric = fetchSshMetrics(service);
                if (metric != null) {
                    repository.saveMetric(service.getId(), metric.getCpuUsage(), "CPU");
                    repository.saveMetric(service.getId(), metric.getRamUsage(), "RAM");
                    LOG.info("Resources for {}: CPU={}%, RAM={}%", service.getName(), metric.getCpuUsage(), metric.getRamUsage());
                }
            }
        } catch (Exception e) {
            LOG.error("Resource polling error: {}", e.getMessage());
        }
    }

    private ResourceMetric fetchSshMetrics(ServiceEntity service) {
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;
        try {
            session = jsch.getSession(service.getSshUser(), service.getServerIp(), 22);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            if (service.getSshKeyPath() != null && !service.getSshKeyPath().isEmpty()) {
                jsch.addIdentity(service.getSshKeyPath());
            } else {
                session.setPassword("password");
            }
            session.connect(5000);

            // Command to get CPU and RAM for a specific process name
            // Using 'ps' to get %cpu and %mem
            String cmd = "ps -C " + service.getName() + " -o %cpu,%mem --no-headers";
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();
            
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.trim().split("\s+");
                double cpu = Double.parseDouble(parts[0]);
                double ram = Double.parseDouble(parts[1]);
                return new ResourceMetric(cpu, ram);
            }
        } catch (Exception e) {
            LOG.warn("Could not fetch metrics for {}: {}", service.getName(), e.getMessage());
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
        return null;
    }
}
