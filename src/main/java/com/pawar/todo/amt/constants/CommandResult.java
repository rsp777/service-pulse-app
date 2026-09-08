package com.pawar.todo.amt.constants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandResult {

	private static final Logger logger = LoggerFactory.getLogger(CommandResult.class);

	private String requestId;
	private String status;
	private String output;
	private String error;

	public String toJson() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize command result for request {}", requestId, e);
			return "{}"; // Return an empty JSON object in case of error
		}
	}

}
