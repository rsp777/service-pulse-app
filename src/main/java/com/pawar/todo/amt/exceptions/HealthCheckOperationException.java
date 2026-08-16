package com.pawar.todo.amt.exceptions;

public class HealthCheckOperationException extends Exception {
	public HealthCheckOperationException(String message) {
        super(message);
    }
    public HealthCheckOperationException(String message, Throwable cause) {
        super(message, cause);
    }
	public HealthCheckOperationException(String string, String message) {
		 super( message);
	}
}
