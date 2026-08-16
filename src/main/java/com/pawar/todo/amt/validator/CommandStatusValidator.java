package com.pawar.todo.amt.validator;

import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.ServerStatus;

@Component
public class CommandStatusValidator {
	public void validate(String status) {
		try {
			CommandStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid command status: " + status);
		}
	}
}
