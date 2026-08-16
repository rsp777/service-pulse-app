package com.pawar.todo.amt.validator;

import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.constants.ServerStatus;

@Component
public class HealthCheckStatusValidator {
	public void validate(String status) {
		try {
			HealthCheckStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid healtcheck status: " + status);
		}
	}
}
