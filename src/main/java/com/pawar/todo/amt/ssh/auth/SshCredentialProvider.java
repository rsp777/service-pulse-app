package com.pawar.todo.amt.ssh.auth;

import java.io.IOException;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

/**
 * SPI for providing SSH credentials (static private keys or dynamic OpenSSH certificates)
 * to the SSH connection pipeline.
 */
public interface SshCredentialProvider {

    /**
     * Returns an sshj KeyProvider configured with the active key (and certificate, if applicable).
     */
    KeyProvider getKeyProvider(SSHClient client) throws IOException;

    /**
     * Returns active credential metadata (e.g., auth mode, key type, cert serial, expiration).
     */
    SshCredentialMetadata getMetadata();

    /**
     * Returns true if credentials are ready for SSH authentication.
     */
    boolean isReady();
}
