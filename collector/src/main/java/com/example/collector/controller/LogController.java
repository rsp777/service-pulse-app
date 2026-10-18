package com.example.collector.controller;

import com.example.collector.ServiceRepository;
import com.jcraft.jsch.*;
import io.micronaut.http.annotation.*;
import io.micronaut.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.stream.Collectors;

@Controller("/logs")
public class LogController {
    private static final Logger LOG = LoggerFactory.getLogger(LogController.class);
    private final ServiceRepository repository;

    public LogController(ServiceRepository repository) {
        this.repository = repository;
    }

    @Get("/{serviceId}")
    public HttpResponse<String> getLogs(int serviceId) {
        LOG.info("Log request for service: {}", serviceId);
        
        // 1. Fetch service entity from DB to get logs_path and server info
        // Using a mock lookup for the POC
        String logsPath = "/var/log/syslog"; // Fallback
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

            // Command to get last 100 lines of the log file
            String command = "tail -n 100 " + logsPath;
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String logs = reader.lines().collect(Collectors.joining("\n"));
            
            return HttpResponse.ok(logs);

        } catch (Exception e) {
            LOG.error("Log retrieval failed for {}: {}", serviceId, e.getMessage());
            return HttpResponse.serverError("Could not retrieve logs: " + e.getMessage());
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }
}
