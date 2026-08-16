package com.pawar.todo.amt.exceptions;

public class ServerOperationException extends Exception {
	public ServerOperationException(String message) {
        super(message);
    }
    public ServerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
