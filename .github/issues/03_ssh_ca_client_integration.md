# Sub-Issue 3: SSH Certificate Authority (CA) Client Integration

## Parent Epic
Part of Epic #16: [Keyless SSH Authentication with Ephemeral Rotating Keys and OpenSSH Certificates](https://github.com/rsp777/service-pulse-app/issues/16)

## Summary
Implement client communication with SSH Certificate Authorities (supporting HashiCorp Vault SSH Secrets Engine, Smallstep `step-ca`, and generic REST CAs) to request and sign short-lived OpenSSH User Certificates for ephemeral public keys. Provide parser and adapter classes to load the signed certificate into the `sshj` authentication pipeline.

## Current State & Problem
- `SshCommandService` only understands raw private keys on disk.
- There is no client or HTTP integration to interact with an SSH CA.
- `sshj` must be configured with an OpenSSH User Certificate (`ssh-ed25519-cert-v01@openssh.com` or `ssh-rsa-cert-v01@openssh.com`) alongside the private key to authenticate against remote `sshd` instances trusting a CA.

## Proposed Implementation

### 1. CA Signer Client Interface
Create `com.pawar.todo.amt.ssh.ca.SshCertificateAuthorityClient`:
```java
public interface SshCertificateAuthorityClient {
    SshSignedCertificate sign(String openSshPublicKey, CertificateSigningOptions options) throws SshCaException;
}
```

### 2. Vault SSH Client Signer
Implement `com.pawar.todo.amt.ssh.ca.vault.VaultSshCertificateAuthorityClient`:
- Uses Spring `RestClient` or `WebClient` to invoke HashiCorp Vault:
  `POST /v1/{ssh-mount}/sign/{role}`
- Payload:
  ```json
  {
    "public_key": "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5...",
    "valid_principals": "ubuntu,ec2-user,servicepulse",
    "ttl": "30m"
  }
  ```
- Extracts `data.signed_key` and serial number from the response.
- Configurable authentication via Vault Token (`X-Vault-Token`), Kubernetes ServiceAccount token, or AWS IAM auth.

### 3. OpenSSH Certificate Model & Parser
Create `com.pawar.todo.amt.ssh.cert.OpenSshCertificate`:
- Parses raw OpenSSH certificate blob according to PROTOCOL.certkeys format.
- Extracts fields:
  - Serial number (`long`)
  - Certificate type (`USER` vs `HOST`)
  - Key ID / Identity string
  - Valid principals list
  - Validity range (`validAfter`, `validBefore` as `Instant`)
  - Signing CA public key
  - Critical options and extensions (`permit-pty`, `permit-agent-forwarding=no`, etc.)

### 4. SSHJ Certificate KeyProvider Adapter
Create `com.pawar.todo.amt.ssh.cert.CertificateKeyProvider`:
- Adapts `EphemeralKeyPair` + `OpenSshCertificate` into `net.schmizz.sshj.userauth.keyprovider.KeyProvider`.
- Ensures `client.authPublickey(username, certKeyProvider)` presents the signed certificate and signs the challenge with the ephemeral private key.

## Files Affected
- `src/main/java/com/pawar/todo/amt/ssh/ca/SshCertificateAuthorityClient.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/ca/vault/VaultSshCertificateAuthorityClient.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/ca/CertificateSigningOptions.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/ca/SshCaException.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/cert/OpenSshCertificate.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/cert/OpenSshCertificateParser.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/cert/CertificateKeyProvider.java` (new)
- `src/test/java/com/pawar/todo/amt/ssh/ca/vault/VaultSshCertificateAuthorityClientTest.java` (new)
- `src/test/java/com/pawar/todo/amt/ssh/cert/OpenSshCertificateParserTest.java` (new)

## Acceptance Criteria
- [ ] Vault SSH client accurately formats requests and parses responses from the Vault SSH signing endpoint.
- [ ] Certificate parser reliably extracts serial number, principals, and validity window timestamps.
- [ ] `CertificateKeyProvider` properly passes certificate identity and ephemeral private key to `sshj` public key authentication.
- [ ] HTTP network failures or CA error responses produce descriptive, typed `SshCaException` without leaking sensitive tokens or keys in logs.
