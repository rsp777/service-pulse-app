package com.pawar.todo.amt.ssh.cert;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Parses OpenSSH wire-format certificates according to PROTOCOL.certkeys.
 */
public final class OpenSshCertificateParser {

    private OpenSshCertificateParser() {
    }

    public static OpenSshCertificate parse(String certString) {
        if (certString == null || certString.isBlank()) {
            throw new IllegalArgumentException("Certificate string cannot be null or blank");
        }

        String[] parts = certString.trim().split("\\s+");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid OpenSSH certificate format: expected '<type> <base64> [comment]'");
        }

        String keyType = parts[0];
        String base64Blob = parts[1];

        byte[] rawBytes;
        try {
            rawBytes = Base64.getDecoder().decode(base64Blob);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid Base64 payload in OpenSSH certificate", e);
        }

        ByteBuffer buf = ByteBuffer.wrap(rawBytes);

        // 1. cert_type string
        String parsedType = readStringUtf8(buf);
        if (!parsedType.equals(keyType)) {
            // Some implementations prefix outer string with key type, inner binary must match
            keyType = parsedType;
        }

        // 2. nonce (string)
        byte[] nonce = readStringBytes(buf);

        // 3. public key fields
        byte[] publicKeyBytes;
        if (keyType.contains("ed25519")) {
            publicKeyBytes = readStringBytes(buf);
        } else if (keyType.contains("rsa")) {
            // RSA public key consists of e (mpint) and n (mpint)
            byte[] e = readStringBytes(buf);
            byte[] n = readStringBytes(buf);
            ByteBuffer rsaBuf = ByteBuffer.allocate(e.length + n.length + 8);
            rsaBuf.putInt(e.length).put(e);
            rsaBuf.putInt(n.length).put(n);
            publicKeyBytes = rsaBuf.array();
        } else {
            publicKeyBytes = readStringBytes(buf);
        }

        // 4. serial (uint64)
        long serial = buf.getLong();

        // 5. type (uint32: 1 = user, 2 = host)
        int certType = buf.getInt();

        // 6. key_id (string)
        String keyId = readStringUtf8(buf);

        // 7. valid principals (string containing packed strings)
        byte[] principalsPayload = readStringBytes(buf);
        List<String> validPrincipals = parseStringList(principalsPayload);

        // 8. valid_after (uint64: epoch seconds)
        long validAfterSec = buf.getLong();
        Instant validAfter = validAfterSec == 0 ? Instant.EPOCH : Instant.ofEpochSecond(validAfterSec);

        // 9. valid_before (uint64: epoch seconds)
        long validBeforeSec = buf.getLong();
        // 0xFFFFFFFFFFFFFFFFL represents "forever" in OpenSSH
        Instant validBefore = (validBeforeSec == -1L || validBeforeSec == Long.MAX_VALUE)
                ? Instant.MAX
                : Instant.ofEpochSecond(validBeforeSec);

        // 10. critical options (string buffer)
        byte[] criticalOptionsBytes = readStringBytes(buf);
        List<String> criticalOptions = parseStringList(criticalOptionsBytes);

        // 11. extensions (string buffer)
        byte[] extensionsBytes = readStringBytes(buf);
        List<String> extensions = parseStringList(extensionsBytes);

        // 12. reserved (string)
        readStringBytes(buf);

        // 13. signature key (string)
        byte[] signatureKeyBytes = readStringBytes(buf);

        return new OpenSshCertificate(
                certString.trim(),
                keyType,
                serial,
                certType,
                keyId,
                validPrincipals,
                validAfter,
                validBefore,
                criticalOptions,
                extensions,
                nonce,
                publicKeyBytes,
                signatureKeyBytes
        );
    }

    private static List<String> parseStringList(byte[] payload) {
        List<String> items = new ArrayList<>();
        if (payload == null || payload.length == 0) {
            return items;
        }
        ByteBuffer buf = ByteBuffer.wrap(payload);
        while (buf.remaining() >= 4) {
            int len = buf.getInt();
            if (len < 0 || len > buf.remaining()) {
                break;
            }
            byte[] strBytes = new byte[len];
            buf.get(strBytes);
            items.add(new String(strBytes, StandardCharsets.UTF_8));
        }
        return items;
    }

    private static byte[] readStringBytes(ByteBuffer buf) {
        if (buf.remaining() < 4) {
            return new byte[0];
        }
        int len = buf.getInt();
        if (len < 0 || len > buf.remaining()) {
            throw new IllegalArgumentException("Corrupt wire string length in OpenSSH certificate: " + len);
        }
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return bytes;
    }

    private static String readStringUtf8(ByteBuffer buf) {
        byte[] bytes = readStringBytes(buf);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
