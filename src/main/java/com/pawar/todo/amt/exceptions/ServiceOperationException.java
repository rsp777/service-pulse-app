package com.pawar.todo.amt.exceptions;

public class ServiceOperationException extends Exception {
	public ServiceOperationException(String message) {
        super(message);
    }
    public ServiceOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
