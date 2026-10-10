package com.example.collector;

import com.jcraft.jsch.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SshProbe implements Probe {
    private static final Logger LOG = LoggerFactory.getLogger(SshProbe.class);
    private final String sshKeyPath = System.getProperty("user.home") + "/.ssh/id_ed25519_ubuntu_dell";
    private final String sshUser = "ravi";

    @Override
    public ProbeResult execute(ServiceEntity service) {
        long start = System.currentTimeMillis();
        try {
            JSch jsch = new JSch();
            jsch.addIdentity(sshKeyPath);
            
            Session session = jsch.getSession(sshUser, service.getUrl(), 22);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(5000);

            // We'll execute a command to get multiple metrics in one go
            // uptime (load), free -m (memory), df (disk)
            String command = "uptime | awk -F'load average:' '{ print $2 }' | cut -d, -f1 | xargs && " +
                             "free -m | awk '/Mem:/ { print $3 }' && " +
                             "df / | awk 'NR==2 { print $5 }' | sed 's/%//'";
            
            String output = executeCommand(session, command);
            session.disconnect();

            long latency = System.currentTimeMillis() - start;
            
            // The lapped output will be:
            // load_avg
            // mem_used
            // disk_percent
            String[] lines = output.split("\\r?\\n");
            
            // Since SshProbe returns a single ProbeResult, we'll log the detail 
            // and save a "composite" success. In a real system, we'd have multiple metric entries.
            // For this MVP, we'll report "UP" and the latency.
            return new ProbeResult(true, latency, "UP (CPU:" + (lines.length > 0 ? lines[0].trim() : "N/A") + ")");
            
        } catch (Exception e) {
            LOG.error("SSH probe failed for {}: {}", service.getUrl(), e.getMessage());
            return new ProbeResult(false, System.currentTimeMillis() - start, "DOWN");
        }
    }

    private String executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
        channel.connect();
        
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        channel.disconnect();
        return output.toString();
    }
}
