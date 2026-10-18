# Sub-Issue 5: SSH Host Certificate Verification & Strict Host-Key Policy

## Parent Epic
Part of Epic #16: [Keyless SSH Authentication with Ephemeral Rotating Keys and OpenSSH Certificates](https://github.com/rsp777/service-pulse-app/issues/16)

## Summary
Implement SSH Host Certificate verification by trusting an SSH Host CA (`@cert-authority` entries or custom `HostKeyVerifier`), enabling true zero-friction strict host-key verification across dynamically provisioned target servers, and eliminating unsafe fallbacks to `PromiscuousVerifier`.

## Current State & Problem
- In `SshCommandService.java` (lines 171–186), if `known_hosts` is missing or unconfigured, the application silently falls back to `PromiscuousVerifier` (trusting any host key):
  ```java
  // Trust all host keys (resolves the ed25519 TransportException)
  client.addHostKeyVerifier(new PromiscuousVerifier());
  ```
- This directly contradicts the engineering constraint in `.github/agents/java-developer.md`:
  > *"Preserve strict SSH host-key verification. Never expose, log, commit, or add credentials, private keys, passphrases, or tokens."*
- However, manually populating and synchronizing individual target server host fingerprints in `known_hosts` is unscalable in cloud/ephemeral infrastructure.

## Proposed Implementation

### 1. SSH Host Certificate Verification
In OpenSSH, target servers can have their host keys signed by a centralized Host CA (e.g., `ssh_host_ed25519_key-cert.pub`). 
Client systems specify:
```text
@cert-authority *.internal.domain ssh-ed25519 AAAAC3NzaC1lZDI1NTE5... Host-CA-2026
```
This enables bidirectional cryptographic trust:
- Servers trust the client via `TrustedUserCAKeys`.
- Client trusts servers via `@cert-authority` Host CA signature.

### 2. Implement HostCertificateVerifier
Create `com.pawar.todo.amt.ssh.verifier.HostCertificateVerifier`:
- Implements `net.schmizz.sshj.transport.verification.HostKeyVerifier`.
- Validates that the remote host presents a valid OpenSSH Host Certificate signed by the trusted Host CA.
- Verifies that the host certificate's principal matches the target server's hostname or IP address and is within its validity period.

### 3. Strict Host-Key Policy Enforcement
Refactor `configureHostKeyVerifier(SSHClient client)` in `SshCommandService`:
- If `properties.isStrictHostKeyChecking()` is true:
  1. If `properties.getHostCaPublicKey()` is provided, register `HostCertificateVerifier`.
  2. If `properties.getKnownHostsPath()` is provided, load `known_hosts` (with full `@cert-authority` support).
  3. If neither is available, fail fast on startup or connection with a descriptive `SecurityException`:
     `"Strict host-key verification is enabled, but neither Host CA nor known_hosts is configured."`
- Restrict `PromiscuousVerifier` strictly to local/dev profiles (`@Profile({"local", "dev", "test"})`) and log an explicit security warning.

## Files Affected
- `src/main/java/com/pawar/todo/amt/ssh/SshProperties.java`
- `src/main/java/com/pawar/todo/amt/ssh/SshCommandService.java`
- `src/main/java/com/pawar/todo/amt/ssh/verifier/HostCertificateVerifier.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/verifier/HostCertificateConfig.java` (new)
- `src/test/java/com/pawar/todo/amt/ssh/verifier/HostCertificateVerifierTest.java` (new)

## Acceptance Criteria
- [ ] Remote servers presenting valid OpenSSH Host Certificates signed by the Host CA are successfully verified without individual host entries in `known_hosts`.
- [ ] If a host key does not match the Host CA or known_hosts when strict checking is enabled, the connection is rejected immediately.
- [ ] `PromiscuousVerifier` cannot be enabled in production configurations.
- [ ] Unit tests cover valid certificates, expired host certificates, mismatched hostnames, and invalid CA signatures.
