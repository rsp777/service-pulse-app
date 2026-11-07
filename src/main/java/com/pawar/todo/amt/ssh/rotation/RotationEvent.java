package com.pawar.todo.amt.ssh.rotation;

import java.time.Instant;

/**
 * Event emitted upon SSH certificate rotation or rotation error.
 */
public class RotationEvent {

    public enum Type {
        SUCCESS,
        FAILURE,
        RETRY,
        EXPIRED
    }

    private final Type type;
    private final long serial;
    private final String keyId;
    private final Instant timestamp;
    private final long durationMs;
    private final String errorMessage;

    public RotationEvent(Type type, long serial, String keyId, Instant timestamp, long durationMs, String errorMessage) {
        this.type = type;
        this.serial = serial;
        this.keyId = keyId;
        this.timestamp = timestamp;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
    }

    public static RotationEvent success(long serial, String keyId, long durationMs) {
        return new RotationEvent(Type.SUCCESS, serial, keyId, Instant.now(), durationMs, null);
    }

    public static RotationEvent failure(String errorMessage, long durationMs) {
        return new RotationEvent(Type.FAILURE, 0L, null, Instant.now(), durationMs, errorMessage);
    }

    public static RotationEvent retry(String errorMessage, long durationMs) {
        return new RotationEvent(Type.RETRY, 0L, null, Instant.now(), durationMs, errorMessage);
    }

    public Type getType() {
        return type;
    }

    public long getSerial() {
        return serial;
    }

    public String getKeyId() {
        return keyId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "RotationEvent{" +
                "type=" + type +
                ", serial=" + serial +
                ", keyId='" + keyId + '\'' +
                ", timestamp=" + timestamp +
                ", durationMs=" + durationMs +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
