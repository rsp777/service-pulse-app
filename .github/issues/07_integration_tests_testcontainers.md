# Sub-Issue 7: End-to-End Testcontainers Integration Suite with OpenSSH Server & Mock CA

## Parent Epic
Part of Epic #16: [Keyless SSH Authentication with Ephemeral Rotating Keys and OpenSSH Certificates](https://github.com/rsp777/service-pulse-app/issues/16)

## Summary
Implement a comprehensive, automated end-to-end integration test suite using Testcontainers that launches a real OpenSSH daemon configured exclusively with `TrustedUserCAKeys`. Validate certificate-based authentication, command execution, log streaming, and mid-flight key rotation without relying on static `authorized_keys` or external cloud services.

## Current State & Problem
- Current SSH unit tests rely primarily on mocks or assume pre-existing test keys on developer machines.
- Without a real OpenSSH server configured with CA trust, regressions in certificate parsing, key exchange, or `sshj` certificate negotiation cannot be caught in CI.

## Proposed Implementation

### 1. Testcontainers OpenSSH Test Harness
Create `com.pawar.todo.amt.ssh.it.OpenSshCaContainer`:
- Runs an Alpine-based OpenSSH daemon container.
- Mounts generated Test User CA public key to `/etc/ssh/user_ca.pub`.
- Configures `/etc/ssh/sshd_config`:
  ```text
  AuthorizedKeysFile none
  TrustedUserCAKeys /etc/ssh/user_ca.pub
  PasswordAuthentication no
  PubkeyAuthentication yes
  LogLevel DEBUG3
  ```
- Generates a Host Certificate signed by a Test Host CA and mounts it as `HostCertificate /etc/ssh/ssh_host_ed25519_key-cert.pub`.
- Exposes mapped SSH port dynamically.

### 2. Test CA Signer Utility
Create `com.pawar.todo.amt.ssh.it.TestSshCertificateAuthority`:
- Generates a local root CA keypair for the test run.
- Signs user and host certificates dynamically with configurable TTLs, principals, and options.
- Enables negative testing (e.g. generating expired certificates or certificates signed by an untrusted CA).

### 3. End-to-End Test Scenarios
Implement `com.pawar.todo.amt.ssh.it.KeylessSshIntegrationTest`:
- **Test 1: Successful Keyless Execution:** `SshCommandService.execute()` successfully executes `echo 'pulse-ok'` using an ephemeral key and signed certificate. (Zero entries in `authorized_keys`).
- **Test 2: Rejection of Untrusted CA:** Certificates signed by an unknown CA are rejected with `UserAuthException`.
- **Test 3: Rejection of Expired Certificate:** Certificates with `validBefore` in the past are rejected by sshd.
- **Test 4: Seamless Rotation During Streaming:** Start a continuous log stream (`tail -f`), trigger a certificate rotation in the rotation engine, and verify the stream continues without disconnection while new commands use the fresh certificate.
- **Test 5: Host Key Verification:** Strict host key checking verifies the server's Host Certificate; connection fails if Host CA public key is corrupted.

## Files Affected
- `pom.xml` (ensure Testcontainers dependencies are scoped for test)
- `src/test/java/com/pawar/todo/amt/ssh/it/OpenSshCaContainer.java` (new)
- `src/test/java/com/pawar/todo/amt/ssh/it/TestSshCertificateAuthority.java` (new)
- `src/test/java/com/pawar/todo/amt/ssh/it/KeylessSshIntegrationTest.java` (new)

## Acceptance Criteria
- [ ] Integration tests run automatically in Maven `verify` / `test` phase when Docker is available.
- [ ] Validates that OpenSSH server requires zero entries in `authorized_keys` and authenticates purely via CA certificates.
- [ ] Negative test cases confirm expired or untrusted certificates fail closed.
- [ ] Rotation test proves zero disruption to active SSH sessions.
