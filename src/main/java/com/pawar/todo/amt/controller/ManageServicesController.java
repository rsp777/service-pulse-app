package com.pawar.todo.amt.controller;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.service.ManageServices;

@RestController
@RequestMapping("/api/manage-services")
public class ManageServicesController {

	private static final Logger logger = LoggerFactory.getLogger(ManageServicesController.class);
	private ManageServices manageServices;
	private final Executor serviceActionStreamExecutor;
	
	@Autowired
	public ManageServicesController(@Qualifier("logStreamExecutor") Executor serviceActionStreamExecutor) {
		this.serviceActionStreamExecutor = serviceActionStreamExecutor;
	}

	@Autowired
	public void setManageServices(ManageServices manageServices) {
		this.manageServices = manageServices;
	}

	@GetMapping(value = "/{action}-all-service/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamAllServices(@PathVariable String action, @RequestParam Integer serverId) {
		SseEmitter emitter = new SseEmitter(0L);
		AtomicBoolean stopped = new AtomicBoolean();
		emitter.onCompletion(() -> stopped.set(true));
		emitter.onTimeout(() -> stopped.set(true));
		emitter.onError(error -> stopped.set(true));
		try {
			serviceActionStreamExecutor.execute(() -> {
				try {
					manageServices.streamAllServices(serverId, action,
							line -> send(emitter, "output", line, stopped), stopped);
					if (!stopped.get()) {
						emitter.send(SseEmitter.event().name("complete").data("Lifecycle action completed"));
						emitter.complete();
					}
				} catch (Exception exception) {
					if (!stopped.get()) {
						send(emitter, "error", scriptUnavailableMessage(exception), stopped);
						emitter.complete();
					}
				}
			});
		} catch (RejectedExecutionException exception) {
			emitter.completeWithError(new IllegalStateException("Too many active service actions", exception));
		}
		return emitter;
	}


	private String scriptUnavailableMessage(Exception exception) {
		if (exception instanceof PathOperationException || exception instanceof ResourceNotFoundException) {
			return "SCRIPTS_UNAVAILABLE: Required script configuration is unavailable for this server: "
					+ exception.getMessage();
		}
		return exception.getMessage();
	}
	private void send(SseEmitter emitter, String eventName, String value, AtomicBoolean stopped) {
		if (stopped.get()) {
			return;
		}
		try {
			emitter.send(SseEmitter.event().name(eventName).data(value == null ? "Unknown lifecycle error" : value));
		} catch (IOException exception) {
			stopped.set(true);
		}
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