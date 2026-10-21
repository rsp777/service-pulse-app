package com.pawar.todo.amt.ssh.key;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;

/**
 * Encodes Java PublicKey instances (Ed25519 and RSA) into the standard OpenSSH wire format
 * according to RFC 4253.
 */
public final class OpenSshKeyEncoder {

    private OpenSshKeyEncoder() {
    }

    public static String encodePublicKey(PublicKey publicKey, String comment) {
        String cleanComment = (comment == null || comment.isBlank()) ? "service-pulse-app" : comment.trim();
        byte[] wireBytes = encodePublicKeyBytes(publicKey);
        String base64Blob = Base64.getEncoder().encodeToString(wireBytes);
        String keyType = resolveKeyType(publicKey);
        return keyType + " " + base64Blob + " " + cleanComment;
    }

    public static byte[] encodePublicKeyBytes(PublicKey publicKey) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if ("Ed25519".equalsIgnoreCase(publicKey.getAlgorithm()) || "EdDSA".equalsIgnoreCase(publicKey.getAlgorithm())) {
                writeString(out, "ssh-ed25519".getBytes(StandardCharsets.US_ASCII));
                byte[] rawBytes = extractEd25519RawPublicKey(publicKey);
                writeString(out, rawBytes);
            } else if (publicKey instanceof RSAPublicKey rsaKey) {
                writeString(out, "ssh-rsa".getBytes(StandardCharsets.US_ASCII));
                writeMpint(out, rsaKey.getPublicExponent());
                writeMpint(out, rsaKey.getModulus());
            } else {
                throw new IllegalArgumentException("Unsupported key algorithm for OpenSSH encoding: " + publicKey.getAlgorithm());
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode OpenSSH public key", e);
        }
    }

    public static String resolveKeyType(PublicKey publicKey) {
        if ("Ed25519".equalsIgnoreCase(publicKey.getAlgorithm()) || "EdDSA".equalsIgnoreCase(publicKey.getAlgorithm())) {
            return "ssh-ed25519";
        } else if (publicKey instanceof RSAPublicKey) {
            return "ssh-rsa";
        }
        throw new IllegalArgumentException("Unsupported public key algorithm: " + publicKey.getAlgorithm());
    }

    private static byte[] extractEd25519RawPublicKey(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        if (encoded == null || encoded.length < 32) {
            throw new IllegalArgumentException("Invalid Ed25519 public key encoding");
        }
        // In standard X.509 SubjectPublicKeyInfo for Ed25519, the last 32 bytes contain the raw public key point
        return Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length);
    }

    private static void writeString(ByteArrayOutputStream out, byte[] bytes) throws IOException {
        out.write(ByteBuffer.allocate(4).putInt(bytes.length).array());
        out.write(bytes);
    }

    private static void writeMpint(ByteArrayOutputStream out, BigInteger val) throws IOException {
        if (val.equals(BigInteger.ZERO)) {
            out.write(ByteBuffer.allocate(4).putInt(0).array());
            return;
        }
        byte[] bytes = val.toByteArray();
        writeString(out, bytes);
    }
}
