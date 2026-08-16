package com.pawar.todo.amt.exceptions;

public class PathOperationException extends Exception {
	public PathOperationException(String message) {
        super(message);
    }
    public PathOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
