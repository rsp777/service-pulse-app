package com.pawar.todo.amt.exceptions;

public class ScriptOperationException extends Exception {
	public ScriptOperationException(String message) {
        super(message);
    }
    public ScriptOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
