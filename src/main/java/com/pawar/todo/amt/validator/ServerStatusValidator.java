package com.pawar.todo.amt.validator;

import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.ServerStatus;

@Component
public class ServerStatusValidator {
	public void validate(String status) {
		try {
			ServerStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid server status: " + status);
		}
	}
}
