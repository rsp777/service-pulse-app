package com.pawar.todo.amt.ssh.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pawar.todo.amt.ssh.auth.CaCertificateCredentialProvider;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificateParser;
import com.pawar.todo.amt.ssh.key.DefaultEphemeralKeyPairGenerator;
import com.pawar.todo.amt.ssh.key.EphemeralKeyPair;
import com.pawar.todo.amt.ssh.key.KeyAlgorithm;
import com.pawar.todo.amt.ssh.rotation.ActiveSshCredential;
import com.pawar.todo.amt.ssh.verifier.HostCertificateVerifier;

import net.schmizz.sshj.SSHClient;

class KeylessSshIntegrationTest {

    private TestSshCertificateAuthority userCa;
    private TestSshCertificateAuthority hostCa;
    private DefaultEphemeralKeyPairGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        userCa = new TestSshCertificateAuthority();
        hostCa = new TestSshCertificateAuthority();
        keyGenerator = new DefaultEphemeralKeyPairGenerator();
    }

    @Test
    void scenario1_validSignedUserCertificate_authenticatesSuccessfully() throws Exception {
        EphemeralKeyPair keyPair = keyGenerator.generateKeyPair(KeyAlgorithm.ED25519);
        String certString = userCa.signUserCertificate(
                keyPair.getPublicKey(), "servicepulse-runner", List.of("servicepulse", "ubuntu"), Duration.ofHours(1)
        );

        OpenSshCertificate cert = OpenSshCertificateParser.parse(certString);
        assertNotNull(cert);
        assertTrue(cert.isUserCertificate());
        assertFalse(cert.isExpired());
        assertEquals("servicepulse-runner", cert.getKeyId());
        assertTrue(cert.getValidPrincipals().contains("servicepulse"));

        ActiveSshCredential credential = new ActiveSshCredential(keyPair, cert);
        CaCertificateCredentialProvider provider = new CaCertificateCredentialProvider(credential);

        assertTrue(provider.isReady());
        assertNotNull(provider.getKeyProvider(new SSHClient()));
    }

    @Test
    void scenario2_rejectionOfUntrustedCaCertificate() {
        TestSshCertificateAuthority rogueCa = new TestSshCertificateAuthority();
        EphemeralKeyPair hostKey = keyGenerator.generateKeyPair(KeyAlgorithm.ED25519);

        // Signed by rogue CA instead of trusted host CA
        String rogueHostCert = rogueCa.signHostCertificate(
                hostKey.getPublicKey(), "rogue-host", List.of("node1.internal"), Duration.ofHours(1)
        );
        OpenSshCertificate parsedRogueCert = OpenSshCertificateParser.parse(rogueHostCert);

        HostCertificateVerifier verifier = new HostCertificateVerifier(hostCa.getCaPublicKeyWire());
        boolean verified = verifier.verifyHostCertificate("node1.internal", 22, parsedRogueCert);

        assertFalse(verified, "Host certificate signed by untrusted CA must be rejected");
    }

    @Test
    void scenario3_rejectionOfExpiredCertificate() {
        EphemeralKeyPair keyPair = keyGenerator.generateKeyPair(KeyAlgorithm.ED25519);
        String expiredCertString = userCa.signExpiredCertificate(
                keyPair.getPublicKey(), "expired-runner", List.of("servicepulse")
        );

        OpenSshCertificate expiredCert = OpenSshCertificateParser.parse(expiredCertString);
        assertTrue(expiredCert.isExpired());

        ActiveSshCredential credential = new ActiveSshCredential(keyPair, expiredCert);
        CaCertificateCredentialProvider provider = new CaCertificateCredentialProvider(credential);

        assertFalse(provider.isReady());
        assertThrows(IOException.class, () -> provider.getKeyProvider(new SSHClient()));
    }

    @Test
    void scenario4_seamlessZeroDowntimeRotation() throws Exception {
        // Step 1: Initial Credential v1
        EphemeralKeyPair keyPair1 = keyGenerator.generateKeyPair(KeyAlgorithm.ED25519);
        String cert1 = userCa.signUserCertificate(keyPair1.getPublicKey(), "runner-v1", List.of("servicepulse"), Duration.ofMinutes(30));
        ActiveSshCredential cred1 = new ActiveSshCredential(keyPair1, OpenSshCertificateParser.parse(cert1));

        CaCertificateCredentialProvider provider = new CaCertificateCredentialProvider(cred1);
        assertEquals(1, provider.getMetadata().getTotalRotations());
        assertEquals(cred1.getSerial(), provider.getCurrentCredential().getSerial());

        // Step 2: Proactive rotation to Credential v2
        EphemeralKeyPair keyPair2 = keyGenerator.generateKeyPair(KeyAlgorithm.ED25519);
        String cert2 = userCa.signUserCertificate(keyPair2.getPublicKey(), "runner-v2", List.of("servicepulse"), Duration.ofMinutes(30));
        ActiveSshCredential cred2 = new ActiveSshCredential(keyPair2, OpenSshCertificateParser.parse(cert2));

        provider.updateCredential(cred2, Duration.ofSeconds(5));

        // Step 3: Verify v2 is now active without downtime
        assertTrue(provider.isReady());
        assertEquals(2, provider.getMetadata().getTotalRotations());
        assertEquals(cred2.getSerial(), provider.getCurrentCredential().getSerial());
        assertEquals("runner-v2", provider.getCurrentCredential().getKeyId());

        // Provider continues to serve key provider cleanly
        assertNotNull(provider.getKeyProvider(new SSHClient()));
    }

    @Test
    void scenario5_hostCertificateStrictVerification_matchesWildcardDomains() {
        EphemeralKeyPair hostKey = keyGenerator.generateKeyPair(KeyAlgorithm.ED25519);
        String hostCertString = hostCa.signHostCertificate(
                hostKey.getPublicKey(), "prod-host", List.of("*.service.internal", "bastion.infra"), Duration.ofHours(24)
        );
        OpenSshCertificate hostCert = OpenSshCertificateParser.parse(hostCertString);

        HostCertificateVerifier verifier = new HostCertificateVerifier(hostCa.getCaPublicKeyWire());

        assertTrue(verifier.verifyHostCertificate("api.service.internal", 22, hostCert));
        assertTrue(verifier.verifyHostCertificate("db.service.internal", 22, hostCert));
        assertTrue(verifier.verifyHostCertificate("bastion.infra", 22, hostCert));
        assertFalse(verifier.verifyHostCertificate("unknown.external.com", 22, hostCert));
    }
}
