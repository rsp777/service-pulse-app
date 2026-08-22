package com.pawar.todo.amt.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.ManageServices;

@RestController
@RequestMapping("/api/manage-services")
public class ManageServicesController {

	private static final Logger logger = LoggerFactory.getLogger(ManageServicesController.class);
	private ManageServices manageServices;
	
	@Autowired
	public void setManageServices(ManageServices manageServices) {
		this.manageServices = manageServices;
	}

	@PostMapping("/start-service")
	public ResponseEntity<ApiResponse<String>> startService(@RequestParam Integer serverId,
			@RequestParam Integer serviceId) {
		try {
			String responseMessage = manageServices.startService(serverId, serviceId);
			logger.info("responseMessage : {}", responseMessage);
			return ResponseEntity.ok(new ApiResponse<>(true, responseMessage, responseMessage));

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
	public ResponseEntity<ApiResponse<String>> stopService(@RequestParam Integer serverId,
			@RequestParam Integer serviceId) {
		try {
			String responseMessage = manageServices.stopService(serverId, serviceId);
			logger.info("responseMessage : {}", responseMessage);
			return ResponseEntity.ok(new ApiResponse<>(true, responseMessage, responseMessage));

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
	public ResponseEntity<ApiResponse<String>> startAllServices(@RequestParam Integer serverId) {
		try {
			String responseMessage = manageServices.startAllServices(serverId);
			logger.info("responseMessage : {}", responseMessage);
			return ResponseEntity.ok(new ApiResponse<>(true, responseMessage, responseMessage));

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
	public ResponseEntity<ApiResponse<String>> stopAllServices(@RequestParam Integer serverId) {
		try {
			String responseMessage = manageServices.stopAllServices(serverId);
			logger.info("responseMessage : {}", responseMessage);
			return ResponseEntity.ok(new ApiResponse<>(true, responseMessage, responseMessage));

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

	@PostMapping("/restart-all-service")
	public ResponseEntity<ApiResponse<String>> restartAllServices(@RequestParam Integer serverId) {
		try {
			String responseMessage = manageServices.restartAllServices(serverId);
			return ResponseEntity.ok(new ApiResponse<>(true, responseMessage, responseMessage));
		} catch (Exception exception) {
			logger.error("Error restarting all services", exception);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>(false, "Failed to restart services: " + exception.getMessage(), null));
		}
	}

}