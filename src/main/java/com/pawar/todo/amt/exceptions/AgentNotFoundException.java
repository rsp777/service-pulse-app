package com.pawar.todo.amt.exceptions;

public class AgentNotFoundException extends Exception {
	public AgentNotFoundException(String message) {
        super(message);
    }
    public AgentNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
