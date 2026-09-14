package com.pawar.todo.amt.validator;

import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.ScriptExtension;

@Component
public class ScriptExtensionValidator {
	public void validate(String scriptExtension) {
		try {
			ScriptExtension.valueOf(scriptExtension.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid scripte extension: " + scriptExtension);
		}
	}
}
