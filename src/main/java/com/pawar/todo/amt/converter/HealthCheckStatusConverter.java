package com.pawar.todo.amt.converter;

import org.springframework.stereotype.Component;
import com.pawar.todo.amt.constants.HealthCheckStatus;

@Component
public class HealthCheckStatusConverter {
	public HealthCheckStatus toEnum(String status) {
		return HealthCheckStatus.valueOf(status.toUpperCase());
	}

	public String toString(HealthCheckStatus status) {
		return status.name();
	}
}