package com.pawar.todo.amt.ssh.key;

/**
 * Interface for generating in-memory ephemeral keypairs for SSH authentication.
 */
public interface EphemeralKeyPairGenerator {

    EphemeralKeyPair generateKeyPair(KeyAlgorithm algorithm);
}
