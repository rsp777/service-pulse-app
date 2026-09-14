package com.pawar.todo.amt.validator;

import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.HttpMethodName;

@Component
public class HttpMethodNameValidator {
	public void validate(String methodName) {
		try {
			HttpMethodName.valueOf(methodName.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid http method name: " + methodName);
		}
	}
}
