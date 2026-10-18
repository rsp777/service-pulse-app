# Sub-Issue 1: SshCredentialProvider Abstraction & Configuration Schema

## Parent Epic
Part of Epic #16: [Keyless SSH Authentication with Ephemeral Rotating Keys and OpenSSH Certificates](https://github.com/rsp777/service-pulse-app/issues/16)

## Summary
Decouple `SshCommandService` from static file loading by introducing a pluggable `SshCredentialProvider` SPI. Extend `SshProperties` to support multiple authentication modes (`STATIC`, `CA_CERTIFICATE`), maintaining full backwards compatibility for existing deployments.

## Current State & Problem
- In `src/main/java/com/pawar/todo/amt/ssh/SshCommandService.java` (lines 154–163), private keys are strictly loaded from `properties.getPrivateKeyPath()`:
  ```java
  KeyProvider keyProvider;
  if (StringUtils.hasText(properties.getPrivateKeyPassphrase())) {
      keyProvider = client.loadKeys(properties.getPrivateKeyPath(), properties.getPrivateKeyPassphrase());
  } else {
      keyProvider = client.loadKeys(properties.getPrivateKeyPath());
  }
  client.authPublickey(properties.getUsername(), keyProvider);
  ```
- In `src/main/java/com/pawar/todo/amt/ssh/SshProperties.java`, properties are tightly coupled to a single private key file path.
- Cannot dynamically switch authentication mechanisms or supply in-memory certificates.

## Proposed Implementation

### 1. SPI Design
Create `com.pawar.todo.amt.ssh.auth.SshCredentialProvider`:
```java
public interface SshCredentialProvider {
    /**
     * Returns an sshj KeyProvider configured with the active key (and certificate, if applicable).
     */
    KeyProvider getKeyProvider(SSHClient client) throws IOException;

    /**
     * Returns active credential metadata (e.g., auth mode, key type, cert serial, expiration).
     */
    SshCredentialMetadata getMetadata();

    /**
     * Refreshes or validates credential readiness.
     */
    boolean isReady();
}
```

### 2. Implementations
- `StaticKeyCredentialProvider`: Wraps current file loading logic from `properties.getPrivateKeyPath()`.
- Define placeholder `CaCertificateCredentialProvider` to be populated in subsequent sub-issues.

### 3. SshProperties Extensions
Extend `com.pawar.todo.amt.ssh.SshProperties`:
```yaml
ssh:
  auth-mode: STATIC # STATIC | CA_CERTIFICATE
  username: ubuntu
  # Existing static properties preserved
  private-key-path: /path/to/key
  private-key-passphrase:
  # New CA certificate properties
  ca:
    type: VAULT # VAULT | STEP_CA | REST
    endpoint: http://127.0.0.1:8200
    token: ${SSH_CA_TOKEN:}
    role: servicepulse-runner
    principals:
      - ubuntu
      - ec2-user
      - app-user
    key-algorithm: ED25519 # ED25519 | RSA
    cert-ttl-minutes: 60
    renew-threshold-percentage: 75 # rotate when 75% of TTL has elapsed
```

### 4. Wire SshCommandService
Refactor `SshCommandService` to inject `SshCredentialProvider`:
```java
client.authPublickey(properties.getUsername(), credentialProvider.getKeyProvider(client));
```

## Files Affected
- `src/main/java/com/pawar/todo/amt/ssh/SshProperties.java`
- `src/main/java/com/pawar/todo/amt/ssh/SshCommandService.java`
- `src/main/java/com/pawar/todo/amt/ssh/auth/SshCredentialProvider.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/auth/StaticKeyCredentialProvider.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/auth/SshCredentialMetadata.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/auth/SshAuthMode.java` (new enum: `STATIC`, `CA_CERTIFICATE`)
- `src/test/java/com/pawar/todo/amt/ssh/auth/StaticKeyCredentialProviderTest.java` (new)

## Acceptance Criteria
- [ ] `SshCredentialProvider` SPI is defined and injected into `SshCommandService`.
- [ ] When `ssh.auth-mode=STATIC` (default), behavior is 100% identical to current static key loading.
- [ ] `SshProperties` validation gracefully validates mandatory fields based on active `auth-mode`.
- [ ] Unit tests verify fallback and correct provider instantiation via Spring Boot auto-configuration.
