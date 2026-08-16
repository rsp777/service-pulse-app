package com.pawar.todo.amt.exceptions;

public class AgentOperationException extends Exception {
	public AgentOperationException(String message) {
        super(message);
    }
    public AgentOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
