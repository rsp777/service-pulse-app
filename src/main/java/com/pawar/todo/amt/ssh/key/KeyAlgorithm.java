package com.pawar.todo.amt.ssh.key;

public enum KeyAlgorithm {
    ED25519("Ed25519", "ssh-ed25519"),
    RSA("RSA", "ssh-rsa");

    private final String jcaName;
    private final String sshKeyType;

    KeyAlgorithm(String jcaName, String sshKeyType) {
        this.jcaName = jcaName;
        this.sshKeyType = sshKeyType;
    }

    public String getJcaName() {
        return jcaName;
    }

    public String getSshKeyType() {
        return sshKeyType;
    }

    public static KeyAlgorithm fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ED25519;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.contains("RSA")) {
            return RSA;
        }
        return ED25519;
    }
}
