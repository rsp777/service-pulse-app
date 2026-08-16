package com.pawar.todo.amt.exceptions;

public class HealthCheckNotFoundException extends Exception {
	public HealthCheckNotFoundException(String message) {
        super(message);
    }
    public HealthCheckNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
