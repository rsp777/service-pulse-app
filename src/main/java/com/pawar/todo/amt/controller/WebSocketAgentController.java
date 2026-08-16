package com.pawar.todo.amt.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.TextMessage;

import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.todo.amt.constants.CommandResult;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.WebSocketAgentService;

@Controller
@RestController
@RequestMapping("/api")
public class WebSocketAgentController {

	private static final Logger logger = LoggerFactory.getLogger(WebSocketAgentController.class);
	private CompletableFuture<TextMessage> responseFuture = new CompletableFuture<>();
	private WebSocketAgentService webSocketAgentService;

	@Autowired
	public WebSocketAgentController(WebSocketAgentService webSocketAgentService) {
		this.webSocketAgentService = webSocketAgentService;
	}

	@MessageMapping("/command")
	@SendTo("/topic/command-response")
	public String handleCommand(String command) {
		logger.info("Command  : {}", command);
		return "Processed command: " + command;
	}

	@PostMapping("/send-command")
	public ResponseEntity<ApiResponse<CommandResponseDto>> sendCommandToAgent(@RequestParam Integer agentId,
			@RequestParam String command) {
		try {
			String responseMessage = webSocketAgentService.sendCommand(agentId, command);
			TextMessage response = waitForResponse();

			logger.info("responseMessage : {}", responseMessage);
			logger.info("response : {}", response.getPayload());
			responseFuture = new CompletableFuture<>();
			return ResponseEntity.ok(new ApiResponse<>(true, responseMessage + "|CommandResult : " + response, null));

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

			responseFuture.get();
			return responseFuture.get(200, TimeUnit.SECONDS); // Adjust timeout as needed
		} catch (Exception e) {
			e.printStackTrace();
			return new TextMessage("No response received");
		}
	}

	// This method should be called from your WebSocket handler when a message is
	// received
	public void onMessageReceived(TextMessage message) {
		logger.info("onMessageReceived : {}", message);
		logger.info("responseFuture.isDone() : {}", responseFuture.isDone());
		if (!responseFuture.isDone()) {
			responseFuture.complete(message); // Complete the future with the latest message
		}
	}
}