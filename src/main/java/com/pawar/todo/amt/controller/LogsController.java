package com.pawar.todo.amt.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.TextMessage;

import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.LogsService;

@RestController
@RequestMapping("/api/logs")
public class LogsController {

	private static final Logger logger = LoggerFactory.getLogger(LogsController.class);
	private CompletableFuture<TextMessage> responseFuture = new CompletableFuture<>();

	@Autowired
	private LogsService logsService;

	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping("/view")
	public ResponseEntity<ApiResponse<TextMessage>> viewLogsByService(@RequestParam Integer agentId,
			@RequestParam Integer serviceId) {
		try {
			String responseMessage = logsService.viewLogsByService(agentId, serviceId);
			TextMessage response = waitForResponse();
			responseFuture = new CompletableFuture<>();
			logger.info("responseMessage : {}", responseMessage);
			return ResponseEntity.ok(new ApiResponse<>(true, responseMessage, response));

		} catch (HttpMessageNotReadableException e) {
			logger.error("Error getting api response: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new ApiResponse<>(false, "Failed to get api response: " + e.getMessage(), null));
		} catch (Exception e) {
			logger.error("Error getting api response: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(false, "Failed to get api response: " + e.getMessage(), null));
		}
	}

	private TextMessage waitForResponse() {
		try {
			return responseFuture.get(200, TimeUnit.SECONDS); // Adjust timeout as needed
		} catch (Exception e) {
			e.printStackTrace();
			return new TextMessage("No response received");
		}
	}

	public void onMessageReceived(TextMessage message) {
		logger.debug("onMessageReceived : {}", message);
		if (!responseFuture.isDone()) {
			responseFuture.complete(message); // Complete the future with the latest message
		}
	}
}