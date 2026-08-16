package com.pawar.todo.amt.validator;

import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.AgentStatus;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.HttpMethodName;
import com.pawar.todo.amt.constants.ServerStatus;

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
