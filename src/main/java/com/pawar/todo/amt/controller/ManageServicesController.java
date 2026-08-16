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
import com.pawar.todo.amt.service.ManageServices;
import com.pawar.todo.amt.service.WebSocketAgentService;

@RestController
@RequestMapping("/api/manage-services")
public class ManageServicesController {

	private static final Logger logger = LoggerFactory.getLogger(ManageServicesController.class);
	private CompletableFuture<TextMessage> responseFuture = new CompletableFuture<>();

	
	private ManageServices manageServices;
	
	@Autowired
	public void setManageServices(ManageServices manageServices) {
		this.manageServices = manageServices;
	}

	@PostMapping("/start-service")
	public ResponseEntity<ApiResponse<TextMessage>> startService(@RequestParam Integer agentId,
			@RequestParam Integer serviceId) {
		try {
			String responseMessage = manageServices.startService(agentId, serviceId);
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

	@PostMapping("/stop-service")
	public ResponseEntity<ApiResponse<TextMessage>> stopService(@RequestParam Integer agentId,
			@RequestParam Integer serviceId) {
		try {
			String responseMessage = manageServices.stopService(agentId, serviceId);
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

	@PostMapping("/start-all-service")
	public ResponseEntity<ApiResponse<TextMessage>> startAllServices(@RequestParam Integer agentId) {
		try {
			String responseMessage = manageServices.startAllServices(agentId);
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

	@PostMapping("/stop-all-service")
	public ResponseEntity<ApiResponse<TextMessage>> stopAllServices(@RequestParam Integer agentId) {
		try {
			String responseMessage = manageServices.stopAllServices(agentId);
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

	public TextMessage waitForResponse() {
		try {
			TextMessage responMessage = responseFuture.get(200, TimeUnit.SECONDS);
			logger.debug("waiting for response : {}",responMessage);
			return responMessage; // Adjust timeout as needed
		} catch (Exception e) {
			e.printStackTrace();
			return new TextMessage("No response received");
		}
	}

	public void onMessageReceived(TextMessage message) {
		logger.info("onMessageReceived : {}", message);
		logger.debug("responseFuture.isDone() : {}", responseFuture.isDone());
		if (!responseFuture.isDone()) {
			responseFuture.complete(message); // Complete the future with the latest message
		}
	}
}