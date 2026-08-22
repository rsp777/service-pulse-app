package com.pawar.todo.amt.ssh;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ssh")
public class SshProperties {

    private String username;
    private String privateKeyPath;
    private String privateKeyPassphrase;
    private String knownHostsPath;
    private boolean strictHostKeyChecking = false;
    private int port = 22;
    private int connectTimeoutMillis = 10_000;
    private long commandTimeoutSeconds = 60;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }

    public String getKnownHostsPath() {
        return knownHostsPath;
    }

    public void setKnownHostsPath(String knownHostsPath) {
        this.knownHostsPath = knownHostsPath;
    }

    public boolean isStrictHostKeyChecking() {
        return strictHostKeyChecking;
    }

    public void setStrictHostKeyChecking(boolean strictHostKeyChecking) {
        this.strictHostKeyChecking = strictHostKeyChecking;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public long getCommandTimeoutSeconds() {
        return commandTimeoutSeconds;
    }

    public void setCommandTimeoutSeconds(long commandTimeoutSeconds) {
        this.commandTimeoutSeconds = commandTimeoutSeconds;
    }
}