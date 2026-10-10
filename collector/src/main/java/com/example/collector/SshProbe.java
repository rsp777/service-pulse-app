package com.example.collector;

import com.example.collector.model.ServiceEntity;
import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

public class SshProbe implements Probe {
    private static final Logger LOG = LoggerFactory.getLogger(SshProbe.class);

    @Override
    public ProbeResult execute(ServiceEntity service) {
        long start = System.currentTimeMillis();
        JSch jsch = new JSch();
        Session session = null;
        ChannelExec channel = null;

        try {
            // For POC, we use a simple password or key path from the entity
            // In a real app, we'd handle identity files properly
            session = jsch.getSession(service.getSshUser(), service.getServerIp(), 22);
            
            // Security: In a POC, we skip host key check
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            // We assume the SSH user has a password or key configured in the environment
            // For this implementation, we'll simulate the connection if we can't find keys,
            // but let's try to actually connect if keys are provided.
            if (service.getSshKeyPath() != null && !service.getSshKeyPath().isEmpty()) {
                jsch.addIdentity(service.getSshKeyPath());
            } else {
                // Fallback for POC: simulate password "password" if no key
                session.setPassword("password");
            }

            session.connect(5000);

            // To verify the "Service" is up, we check if the process is running
            // Example command: pgrep -f <service_name>
            String command = "pgrep -f " + service.getName();
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setErrStream(System.err);

            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            String line = reader.readLine();
            long latency = System.currentTimeMillis() - start;

            if (line != null) {
                return new ProbeResult(true, latency, "UP");
            } else {
                return new ProbeResult(false, latency, "DOWN: Process not found");
            }

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            LOG.error("SSH Probe failed for {}: {}", service.getName(), e.getMessage());
            return new ProbeResult(false, latency, "SSH_ERROR: " + e.getMessage());
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }
}
