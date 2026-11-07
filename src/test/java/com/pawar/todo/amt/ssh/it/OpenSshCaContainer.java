package com.pawar.todo.amt.ssh.it;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * Testcontainers harness configuring an OpenSSH server with CA certificate trust.
 */
public class OpenSshCaContainer extends GenericContainer<OpenSshCaContainer> {

    public static final int SSH_PORT = 2222;
    public static final String DEFAULT_USER = "servicepulse";

    public OpenSshCaContainer(String userCaPublicKey) {
        super("linuxserver/openssh-server:latest");
        withExposedPorts(SSH_PORT);
        withEnv("USER_NAME", DEFAULT_USER);
        withEnv("PASSWORD_ACCESS", "false");
        withEnv("USER_CA_PUB", userCaPublicKey != null ? userCaPublicKey : "");
        waitingFor(Wait.forListeningPort());
    }

    public int getSshPort() {
        return getMappedPort(SSH_PORT);
    }

    public String getSshHost() {
        return getHost();
    }
}
