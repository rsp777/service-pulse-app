package com.example.collector.controller;

import com.example.collector.ServiceRepository;
import com.example.collector.model.ServiceEntity;
import com.jcraft.jsch.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

@Controller("/control")
public class ControlController {
    private static final Logger LOG = LoggerFactory.getLogger(ControlController.class);
    private final ServiceRepository repository;

    public ControlController(ServiceRepository repository) {
        this.repository = repository;
    }

    @Post("/execute/{serviceId}/{action}")
    public HttpResponse<String> executeAction(int serviceId, String action) {
        LOG.info("Received control request: Service={}, Action={}", serviceId, action);
        
        // In a real scenario, we'd fetch the full ServiceEntity from repository
        // Here we simulate the fetch of the custom script path
        String scriptPath = "/opt/scripts/restart-nginx.sh"; // Mocked lookup from DB
        String serverIp = "192.168.29.52";
        String sshUser = "ravi";

        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;

        try {
            session = jsch.getSession(sshUser, serverIp, 22);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword("password");
            session.connect(5000);

            // Execute the custom script path found in DB
            String command = "bash " + scriptPath + " " + action;
            LOG.info("Executing custom remote script: {}", command);
            
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.connect();

            return HttpResponse.ok("Successfully triggered custom script " + scriptPath + " with action " + action);

        } catch (Exception e) {
            LOG.error("Control execution failed: {}", e.getMessage());
            return HttpResponse.serverError("Custom script execution failed: " + e.getMessage());
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }
}
