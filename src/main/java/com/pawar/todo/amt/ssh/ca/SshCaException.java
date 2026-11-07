package com.pawar.todo.amt.ssh.ca;

/**
 * Exception thrown when SSH Certificate Authority signing or communication fails.
 * Sanitized to ensure no sensitive tokens or private keys are exposed in error messages.
 */
public class SshCaException extends RuntimeException {

    private final int statusCode;

    public SshCaException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public SshCaException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public SshCaException(String message, int statusCode) {
        super(message + " (HTTP Status: " + statusCode + ")");
        this.statusCode = statusCode;
    }

    public SshCaException(String message, int statusCode, Throwable cause) {
        super(message + " (HTTP Status: " + statusCode + ")", cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
