package com.pawar.todo.amt.exceptions;

public class ServiceHealthStatusOperationException extends Exception {
	public ServiceHealthStatusOperationException(String message) {
        super(message);
    }
    public ServiceHealthStatusOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
