# Sub-Issue 2: Ephemeral In-Memory Keypair Generation & Zeroization Lifecycle

## Parent Epic
Part of Epic #16: [Keyless SSH Authentication with Ephemeral Rotating Keys and OpenSSH Certificates](https://github.com/rsp777/service-pulse-app/issues/16)

## Summary
Implement secure in-memory generation of cryptographic keypairs (Ed25519 and RSA) for "Keyless" SSH authentication. Ensure that no private key material is ever persisted to disk, and sensitive key material is securely zeroized in memory upon key rotation or disposal.

## Current State & Problem
- Current SSH private keys must exist as physical files on disk, referenced by `ssh.privateKeyPath`.
- There is no mechanism in the application to generate ephemeral keypairs in memory or format public keys for OpenSSH CA submission.

## Proposed Implementation

### 1. Ephemeral Keypair Generator
Create `com.pawar.todo.amt.ssh.key.EphemeralKeyPairGenerator`:
- Support algorithms: `ED25519` (default, fastest and most secure, native in Java 17) and `RSA` (4096-bit fallback).
- Use `KeyPairGenerator.getInstance("Ed25519")`.
- Format public keys into standard OpenSSH wire format string (`ssh-ed25519 AAAA... comment` or `ssh-rsa AAAA... comment`) suitable for SSH CA certificate signing requests.

```java
public interface EphemeralKeyPairGenerator {
    EphemeralKeyPair generateKeyPair(KeyAlgorithm algorithm);
}
```

### 2. EphemeralKeyPair & Zeroization Model
Create `com.pawar.todo.amt.ssh.key.EphemeralKeyPair`:
- Hold `KeyPair` reference and generated OpenSSH public key string.
- Implement `AutoCloseable` or `Destroyable` to overwrite/zeroize sensitive private key buffers when rotation replaces the key.
- Provide defensive copies; prevent logging or dumping private key data in `toString()`.

### 3. OpenSSH Wire Format Encoder
Implement `com.pawar.todo.amt.ssh.key.OpenSshKeyEncoder`:
- Serializes `Ed25519PublicKey` and `RSAPublicKey` according to RFC 4253 (OpenSSH public key format).
- Base64-encodes the key blob prefixed by the key type and followed by an identifier (e.g., `service-pulse-app-ephemeral@<timestamp>`).

## Files Affected
- `src/main/java/com/pawar/todo/amt/ssh/key/EphemeralKeyPairGenerator.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/key/DefaultEphemeralKeyPairGenerator.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/key/EphemeralKeyPair.java` (new)
- `src/main/java/com/pawar/todo/amt/ssh/key/KeyAlgorithm.java` (new: `ED25519`, `RSA`)
- `src/main/java/com/pawar/todo/amt/ssh/key/OpenSshKeyEncoder.java` (new)
- `src/test/java/com/pawar/todo/amt/ssh/key/EphemeralKeyPairGeneratorTest.java` (new)
- `src/test/java/com/pawar/todo/amt/ssh/key/OpenSshKeyEncoderTest.java` (new)

## Acceptance Criteria
- [ ] In-memory Ed25519 and RSA key generation succeeds without writing any files to disk.
- [ ] Generated public keys accurately conform to OpenSSH public key wire specification.
- [ ] Calling `close()` or `destroy()` on `EphemeralKeyPair` scrubs sensitive key material from memory.
- [ ] `toString()` on all key classes excludes private key bytes and prints redacted strings.
- [ ] Unit tests validate key generation performance and encoding fidelity across Ed25519 and RSA.
