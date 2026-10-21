package com.pawar.todo.amt.ssh.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pawar.todo.amt.ssh.auth.SshCredentialMetadata;

/**
 * Structured security audit logger for all remote SSH executions.
 * Records operational audit events with cryptographic command digests without ever logging
 * sensitive command arguments, passphrases, or private keys.
 */
@Component
public class SshSecurityAuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("com.pawar.todo.amt.ssh.audit");

    public void logSessionStart(String targetHost, String username, SshCredentialMetadata metadata, String command) {
        String authMode = (metadata != null && metadata.getAuthMode() != null) ? metadata.getAuthMode().name() : "UNKNOWN";
        String certSerial = (metadata != null && metadata.getCertificateSerial() != null) ? metadata.getCertificateSerial() : "NONE";
        String keyId = (metadata != null && metadata.getKeyId() != null) ? metadata.getKeyId() : "NONE";
        int commandLength = command != null ? command.length() : 0;
        String commandHash = hashCommand(command);

        auditLog.info("AUDIT_EVENT=SSH_SESSION_START target_host={} username={} auth_mode={} cert_serial={} key_id={} command_length={} command_hash={}",
                targetHost, username, authMode, certSerial, keyId, commandLength, commandHash);
    }

    public void logSessionEnd(String targetHost, String username, int exitCode, long durationMs) {
        auditLog.info("AUDIT_EVENT=SSH_SESSION_END target_host={} username={} exit_code={} duration_ms={}",
                targetHost, username, exitCode, durationMs);
    }

    public void logSessionFailure(String targetHost, String username, String reason, long durationMs) {
        auditLog.warn("AUDIT_EVENT=SSH_SESSION_FAILED target_host={} username={} reason='{}' duration_ms={}",
                targetHost, username, sanitizeReason(reason), durationMs);
    }

    private static String hashCommand(String command) {
        if (command == null || command.isBlank()) {
            return "empty";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(command.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(hash.length, 16); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return "unavailable";
        }
    }

    private static String sanitizeReason(String reason) {
        if (reason == null) {
            return "unknown";
        }
        return reason.replaceAll("[\r\n\t]", " ").trim();
    }
}
