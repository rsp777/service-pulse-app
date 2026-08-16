package com.pawar.todo.amt.exceptions;

public class CommandOperationException extends Exception {
	public CommandOperationException(String message) {
        super(message);
    }
    public CommandOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
