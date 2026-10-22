package com.pawar.todo.amt.ssh;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.pawar.todo.amt.ssh.auth.SshAuthMode;

@Component
@ConfigurationProperties(prefix = "ssh")
public class SshProperties {

    private SshAuthMode authMode = SshAuthMode.STATIC;
    private String username;
    private String privateKeyPath;
    private String privateKeyPassphrase;
    private String knownHostsPath;
    private boolean strictHostKeyChecking = false;
    private String hostCaPublicKey;
    private int port = 22;
    private int connectTimeoutMillis = 10_000;
    private long commandTimeoutSeconds = 60;

    private CaProperties ca = new CaProperties();

    public SshAuthMode getAuthMode() {
        return authMode;
    }

    public void setAuthMode(SshAuthMode authMode) {
        this.authMode = authMode != null ? authMode : SshAuthMode.STATIC;
    }

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

    public String getHostCaPublicKey() {
        return hostCaPublicKey;
    }

    public void setHostCaPublicKey(String hostCaPublicKey) {
        this.hostCaPublicKey = hostCaPublicKey;
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

    public CaProperties getCa() {
        return ca;
    }

    public void setCa(CaProperties ca) {
        this.ca = ca;
    }

    public static class CaProperties {
        private String type = "VAULT"; // VAULT | STEP_CA | REST
        private String endpoint = "http://127.0.0.1:8200";
        private String token;
        private String role = "servicepulse-runner";
        private String mount = "ssh";
        private List<String> principals = new ArrayList<>(List.of("ubuntu", "ec2-user", "servicepulse"));
        private String keyAlgorithm = "ED25519"; // ED25519 | RSA
        private int certTtlMinutes = 60;
        private int renewThresholdPercentage = 75;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getMount() {
            return mount;
        }

        public void setMount(String mount) {
            this.mount = mount;
        }

        public List<String> getPrincipals() {
            return principals;
        }

        public void setPrincipals(List<String> principals) {
            this.principals = principals != null ? principals : new ArrayList<>();
        }

        public String getKeyAlgorithm() {
            return keyAlgorithm;
        }

        public void setKeyAlgorithm(String keyAlgorithm) {
            this.keyAlgorithm = keyAlgorithm;
        }

        public int getCertTtlMinutes() {
            return certTtlMinutes;
        }

        public void setCertTtlMinutes(int certTtlMinutes) {
            this.certTtlMinutes = certTtlMinutes;
        }

        public int getRenewThresholdPercentage() {
            return renewThresholdPercentage;
        }

        public void setRenewThresholdPercentage(int renewThresholdPercentage) {
            this.renewThresholdPercentage = renewThresholdPercentage;
        }
    }
}