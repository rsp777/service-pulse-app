package com.pawar.todo.amt.exceptions;

public class HttpMethodsOperationException extends Exception {
	public HttpMethodsOperationException(String message) {
        super(message);
    }
    public HttpMethodsOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
