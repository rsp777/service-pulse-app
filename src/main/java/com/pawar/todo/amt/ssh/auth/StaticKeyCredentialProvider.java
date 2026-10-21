package com.pawar.todo.amt.ssh.auth;

import java.io.File;
import java.io.IOException;

import org.springframework.util.StringUtils;

import com.pawar.todo.amt.ssh.SshProperties;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

/**
 * Default credential provider for disk-based static private keys.
 * Preserves 100% backwards compatibility with existing deployments.
 */
public class StaticKeyCredentialProvider implements SshCredentialProvider {

    private final SshProperties properties;

    public StaticKeyCredentialProvider(SshProperties properties) {
        this.properties = properties;
    }

    @Override
    public KeyProvider getKeyProvider(SSHClient client) throws IOException {
        String keyPath = resolvePath(properties.getPrivateKeyPath());
        if (!StringUtils.hasText(keyPath)) {
            throw new IOException("SSH_PRIVATE_KEY_PATH is not configured");
        }
        if (StringUtils.hasText(properties.getPrivateKeyPassphrase())) {
            return client.loadKeys(keyPath, properties.getPrivateKeyPassphrase());
        } else {
            return client.loadKeys(keyPath);
        }
    }

    @Override
    public SshCredentialMetadata getMetadata() {
        return SshCredentialMetadata.staticKey("STATIC", isReady());
    }

    @Override
    public boolean isReady() {
        String keyPath = resolvePath(properties.getPrivateKeyPath());
        if (!StringUtils.hasText(keyPath)) {
            return false;
        }
        File file = new File(keyPath);
        return file.exists() && file.canRead();
    }

    private String resolvePath(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        String normalized = path.trim().replace("/\\", "/").replace("\\/", "/");
        if (normalized.startsWith("~")) {
            normalized = System.getProperty("user.home") + normalized.substring(1);
        }
        return normalized;
    }
}
