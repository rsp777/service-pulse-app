// Issue #26: Support optional/default status [Author: Ravindra Singh Pawar / Antigravity]
package com.pawar.todo.amt.validator;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.pawar.todo.amt.constants.ServerStatus;

@Component
public class ServerStatusValidator {
	public void validate(String status) {
		if (!StringUtils.hasText(status)) {
			return; // Default status will be assigned by backend
		}
		try {
			ServerStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid server status: " + status);
		}
	}
}
