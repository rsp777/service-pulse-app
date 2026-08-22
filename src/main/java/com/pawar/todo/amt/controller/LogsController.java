package com.pawar.todo.amt.controller;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.LogsService;

@RestController
@RequestMapping("/api/logs")
public class LogsController {

	private static final Logger logger = LoggerFactory.getLogger(LogsController.class);
	private final LogsService logsService;
	private final Executor logStreamExecutor;

	public LogsController(LogsService logsService, Executor logStreamExecutor) {
		this.logsService = logsService;
		this.logStreamExecutor = logStreamExecutor;
	}

	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping("/view")
	public ResponseEntity<ApiResponse<String>> viewLogsByService(@RequestParam Integer serverId,
			@RequestParam Integer serviceId) {
		try {
			String responseMessage = logsService.viewLogsByService(serverId, serviceId);
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

	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter streamLogsByService(@RequestParam Integer serverId, @RequestParam Integer serviceId) {
		SseEmitter emitter = new SseEmitter(0L);
		AtomicBoolean stopped = new AtomicBoolean();
		emitter.onCompletion(() -> stopped.set(true));
		emitter.onTimeout(() -> stopped.set(true));
		emitter.onError(error -> stopped.set(true));

		try {
			logStreamExecutor.execute(() -> {
			try {
				logsService.streamLogsByService(serverId, serviceId, line -> {
					try {
						emitter.send(SseEmitter.event().name("log").data(line));
					} catch (IOException exception) {
						stopped.set(true);
					}
				}, stopped);
				if (!stopped.get()) {
					emitter.complete();
				}
			} catch (Exception exception) {
				stopped.set(true);
				emitter.completeWithError(exception);
			}
			});
		} catch (RejectedExecutionException exception) {
			emitter.completeWithError(new IllegalStateException("Too many active log streams", exception));
		}
		return emitter;
	}
}