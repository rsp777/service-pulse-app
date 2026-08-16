package com.pawar.todo.amt.exceptions;

public class ResourceAlreadyExistsException extends Exception {
    public ResourceAlreadyExistsException(String message, Exception e) {
        super(message);
    }

	public ResourceAlreadyExistsException(String message) {
		super(message);
	}

}
