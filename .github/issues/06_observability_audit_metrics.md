# Sub-Issue 6: Actuator Health Indicator, Metrics & Structured Audit Logging

## Parent Epic
Part of Epic #16: [Keyless SSH Authentication with Ephemeral Rotating Keys and OpenSSH Certificates](https://github.com/rsp777/service-pulse-app/issues/16)

## Summary
Provide production-grade observability into the ephemeral key and SSH certificate lifecycle via Spring Boot Actuator health indicators, Micrometer Prometheus metrics, and structured security audit logging recording certificate metadata without exposing secrets.

## Current State & Problem
- Current SSH logs print minimal info (`Starting SSH command on {} as {}`).
- There is no operational visibility into certificate validity, upcoming expiration, or rotation status.
- Ops and monitoring systems (Prometheus, Grafana, Datadog) cannot alert before an SSH certificate expires or if the CA signing service becomes unreachable.

## Proposed Implementation

### 1. Actuator Health Indicator
Create `com.pawar.todo.amt.ssh.actuator.SshCertificateHealthIndicator`:
- Implements `org.springframework.boot.actuate.health.HealthIndicator`.
- Evaluates active certificate state:
  - `UP`: Certificate is valid and remaining TTL > warning threshold (e.g. 10 minutes).
  - `OUT_OF_SERVICE`: Certificate remaining TTL is critical (e.g. < 5 minutes) and renewal is retrying.
  - `DOWN`: Certificate is expired, or rotation engine has failed and no valid certificate exists.
- Reports sanitized metadata in health details:
  ```json
  {
    "status": "UP",
    "details": {
      "authMode": "CA_CERTIFICATE",
      "keyAlgorithm": "ED25519",
      "certificateSerial": "18446744073709551615",
      "keyId": "servicepulse-ephemeral-1718000000",
      "principals": ["ubuntu", "servicepulse"],
      "validUntil": "2026-10-18T22:30:00Z",
      "remainingSeconds": 1740,
      "totalRotations": 14,
      "lastRotatedAt": "2026-10-18T21:30:00Z"
    }
  }
  ```

### 2. Micrometer Metrics
Register metrics with `MeterRegistry`:
- `ssh.cert.ttl.seconds` (Gauge): Current remaining seconds until active certificate expiration.
- `ssh.cert.rotations.total` (Counter, tag: `result=success|failure`): Total rotation attempts.
- `ssh.ca.sign.duration` (Timer): Latency of CA signing requests.
- `ssh.cert.active.valid` (Gauge): 1 if active certificate is valid and within TTL, 0 otherwise.

### 3. Structured Security Audit Logger
Create `com.pawar.todo.amt.ssh.audit.SshSecurityAuditLogger`:
- Logs high-integrity audit events at `INFO` level using a dedicated logger (`com.pawar.todo.amt.ssh.audit`):
  `AUDIT_EVENT=SSH_SESSION_START target_host=192.168.1.100 username=ubuntu cert_serial=839210 key_id=servicepulse-1002 auth_mode=CA_CERTIFICATE command_hash=e3b0c44`
- Never prints private keys, secret tokens, passphrases, or plaintext sensitive payload contents.

## Files Affected
- `src/main/java/com/pawar/todo/amt/ssh/actuator/SshCertificateHealthIndicator.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/actuator/SshMetricsBinder.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/audit/SshSecurityAuditLogger.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/SshCommandService.java`
- `src/test/java/com/pawar/todo/amt/ssh/actuator/SshCertificateHealthIndicatorTest.java` (new)
- `src/test/java/com/pawar/todo/amt/ssh/audit/SshSecurityAuditLoggerTest.java` (new)

## Acceptance Criteria
- [ ] `/actuator/health` accurately reflects certificate health status and non-sensitive metadata.
- [ ] Prometheus metrics endpoint exposes `ssh.cert.ttl.seconds` and rotation metrics.
- [ ] Audit logs emit structured audit events containing certificate serial numbers and key IDs for each SSH execution.
- [ ] Unit tests verify that private keys and tokens are strictly excluded from health responses and log outputs.
