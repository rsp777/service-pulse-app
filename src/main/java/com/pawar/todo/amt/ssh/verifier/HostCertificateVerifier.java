package com.pawar.todo.amt.ssh.verifier;

import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.pawar.todo.amt.ssh.cert.OpenSshCertificate;
import com.pawar.todo.amt.ssh.cert.OpenSshCertificateParser;
import com.pawar.todo.amt.ssh.key.OpenSshKeyEncoder;

import net.schmizz.sshj.transport.verification.HostKeyVerifier;

/**
 * Validates remote host keys against an OpenSSH Host Certificate Authority (Host CA).
 * Verifies CA signature, host principals (including wildcard domain matching), and expiration windows.
 */
public class HostCertificateVerifier implements HostKeyVerifier {

    private static final Logger logger = LoggerFactory.getLogger(HostCertificateVerifier.class);

    private final String trustedHostCaPublicKey;
    private final byte[] trustedHostCaKeyBytes;
    private final List<String> allowedDomains;

    public HostCertificateVerifier(String trustedHostCaPublicKey) {
        this(trustedHostCaPublicKey, Collections.emptyList());
    }

    public HostCertificateVerifier(String trustedHostCaPublicKey, List<String> allowedDomains) {
        this.trustedHostCaPublicKey = cleanHostCaKey(trustedHostCaPublicKey);
        this.trustedHostCaKeyBytes = extractKeyBytes(this.trustedHostCaPublicKey);
        this.allowedDomains = allowedDomains != null ? allowedDomains : Collections.emptyList();
    }

    @Override
    public boolean verify(String hostname, int port, PublicKey key) {
        if (!StringUtils.hasText(hostname) || key == null) {
            logger.warn("Host verification failed: hostname or key is null");
            return false;
        }

        try {
            byte[] presentedKeyBytes = key.getEncoded();
            String encodedKey = OpenSshKeyEncoder.encodePublicKey(key, "");

            // 1. If presented key is a signed OpenSSH Host Certificate
            if (encodedKey.contains("-cert-v01@openssh.com")) {
                OpenSshCertificate cert = OpenSshCertificateParser.parse(encodedKey);
                return verifyHostCertificate(hostname, port, cert);
            }

            // 2. Direct match against trusted Host CA / pinned host key
            if (trustedHostCaKeyBytes != null && presentedKeyBytes != null) {
                if (Arrays.equals(trustedHostCaKeyBytes, presentedKeyBytes) || encodedKey.contains(trustedHostCaPublicKey)) {
                    logger.debug("Direct host key match verified for {}:{}", hostname, port);
                    return true;
                }
            }

            logger.warn("Host key for {}:{} was neither a valid Host Certificate nor a trusted CA match", hostname, port);
            return false;

        } catch (Exception e) {
            logger.error("Error verifying host key for {}:{}: {}", hostname, port, e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> findExistingAlgorithms(String hostname, int port) {
        return Collections.emptyList();
    }

    public boolean verifyHostCertificate(String hostname, int port, OpenSshCertificate cert) {
        if (cert == null) {
            return false;
        }

        // Must be a host certificate (type = 2)
        if (!cert.isHostCertificate()) {
            logger.warn("Host verification rejected for {}:{}: certificate type is not HOST (type={})",
                    hostname, port, cert.getType());
            return false;
        }

        // Check expiration window
        if (cert.isExpired()) {
            logger.warn("Host certificate for {}:{} expired at {}", hostname, port, cert.getValidBefore());
            return false;
        }

        if (cert.getValidAfter() != null && Instant.now().isBefore(cert.getValidAfter())) {
            logger.warn("Host certificate for {}:{} is not yet valid (validAfter={})",
                    hostname, port, cert.getValidAfter());
            return false;
        }

        // Verify CA signature key matches our trusted Host CA
        if (trustedHostCaKeyBytes != null && cert.getSignatureKeyBytes().length > 0) {
            if (!Arrays.equals(trustedHostCaKeyBytes, cert.getSignatureKeyBytes())) {
                logger.warn("Host certificate for {}:{} was not signed by the trusted Host CA", hostname, port);
                return false;
            }
        }

        // Validate hostname matches principals list
        List<String> principals = cert.getValidPrincipals();
        if (principals.isEmpty()) {
            // In OpenSSH, empty principals list in cert means certificate is valid for any host
            logger.debug("Host certificate has wildcard/empty principals list, accepting for {}", hostname);
            return true;
        }

        for (String principal : principals) {
            if (matchesHostname(hostname, principal)) {
                logger.debug("Host certificate verified for {}:{} (matched principal '{}')", hostname, port, principal);
                return true;
            }
        }

        logger.warn("Host certificate principals {} do not match target hostname '{}'", principals, hostname);
        return false;
    }

    private boolean matchesHostname(String targetHostname, String pattern) {
        if (pattern == null || targetHostname == null) {
            return false;
        }
        if (pattern.equalsIgnoreCase(targetHostname)) {
            return true;
        }
        // Wildcard match (e.g. *.internal.domain)
        if (pattern.startsWith("*.")) {
            String suffix = pattern.substring(1); // .internal.domain
            return targetHostname.toLowerCase().endsWith(suffix.toLowerCase());
        }
        return false;
    }

    private static String cleanHostCaKey(String raw) {
        if (raw == null) {
            return "";
        }
        String cleaned = raw.trim();
        // Remove @cert-authority prefix if present
        if (cleaned.startsWith("@cert-authority")) {
            cleaned = cleaned.substring("@cert-authority".length()).trim();
        }
        return cleaned;
    }

    private static byte[] extractKeyBytes(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            return null;
        }
        String[] parts = rawKey.split("\\s+");
        for (String part : parts) {
            try {
                byte[] decoded = java.util.Base64.getDecoder().decode(part);
                if (decoded.length >= 32) {
                    return decoded;
                }
            } catch (IllegalArgumentException e) {
                // not base64, continue
            }
        }
        return null;
    }
}
