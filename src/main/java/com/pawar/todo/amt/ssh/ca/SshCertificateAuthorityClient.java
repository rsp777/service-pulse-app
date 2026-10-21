package com.pawar.todo.amt.ssh.ca;

/**
 * Client SPI for communicating with an SSH Certificate Authority (e.g. HashiCorp Vault, step-ca, REST CA).
 */
public interface SshCertificateAuthorityClient {

    /**
     * Sends an OpenSSH public key to the CA to be signed into an OpenSSH User Certificate.
     *
     * @param openSshPublicKey Standard RFC 4253 OpenSSH public key wire string
     * @param options Signing options including valid principals, TTL, and key ID
     * @return Signed certificate container
     * @throws SshCaException If CA signing fails or the CA service is unreachable
     */
    SshSignedCertificate sign(String openSshPublicKey, CertificateSigningOptions options) throws SshCaException;
}
