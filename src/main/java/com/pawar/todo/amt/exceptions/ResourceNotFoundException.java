package com.pawar.todo.amt.exceptions;

public class ResourceNotFoundException extends Exception {
    public ResourceNotFoundException(String message) {
        super(message);
    }

	public ResourceNotFoundException(String message, Exception e) {
		super(message,e);
		
	}

}
