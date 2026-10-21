package com.pawar.todo.amt.ssh.cert;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import com.pawar.todo.amt.ssh.key.EphemeralKeyPair;

import net.schmizz.sshj.common.KeyType;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

/**
 * Adapts an EphemeralKeyPair and signed OpenSshCertificate into sshj's KeyProvider interface.
 */
public class CertificateKeyProvider implements KeyProvider {

    private final EphemeralKeyPair ephemeralKeyPair;
    private final OpenSshCertificate certificate;

    public CertificateKeyProvider(EphemeralKeyPair ephemeralKeyPair, OpenSshCertificate certificate) {
        if (ephemeralKeyPair == null || certificate == null) {
            throw new IllegalArgumentException("EphemeralKeyPair and OpenSshCertificate must not be null");
        }
        this.ephemeralKeyPair = ephemeralKeyPair;
        this.certificate = certificate;
    }

    @Override
    public PrivateKey getPrivate() throws IOException {
        try {
            return ephemeralKeyPair.getPrivateKey();
        } catch (IllegalStateException e) {
            throw new IOException("Private key has been destroyed", e);
        }
    }

    @Override
    public PublicKey getPublic() throws IOException {
        try {
            return ephemeralKeyPair.getPublicKey();
        } catch (IllegalStateException e) {
            throw new IOException("Public key has been destroyed", e);
        }
    }

    @Override
    public KeyType getType() throws IOException {
        String typeStr = certificate.getKeyType();
        try {
            return KeyType.fromString(typeStr);
        } catch (IllegalArgumentException e) {
            // Fallback for cert type string to base algorithm type if needed
            String baseType = typeStr.replace("-cert-v01@openssh.com", "");
            return KeyType.fromString(baseType);
        }
    }

    public EphemeralKeyPair getEphemeralKeyPair() {
        return ephemeralKeyPair;
    }

    public OpenSshCertificate getCertificate() {
        return certificate;
    }

    public String getRawCertificate() {
        return certificate.getRawString();
    }
}
