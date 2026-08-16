package com.pawar.todo.amt.controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pawar.app.healthcheck.dto.HealthCheckRequestDto;
import com.pawar.app.healthcheck.dto.HealthCheckResponseDto;
import com.pawar.todo.amt.exceptions.HealthCheckNotFoundException;
import com.pawar.todo.amt.exceptions.HealthCheckOperationException;
import com.pawar.todo.amt.service.HealthCheckService;

@RestController
@RequestMapping("/api/healthcheck")
public class HealthCheckController {

	private static final Logger logger = LoggerFactory.getLogger(HealthCheckController.class);
	private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

	private final HealthCheckService healthCheckService;

	public HealthCheckController(HealthCheckService healthCheckService) {
		this.healthCheckService = healthCheckService;
	}

	@PostMapping
	public ResponseEntity<HealthCheckResponseDto> createHealthCheck(@RequestBody HealthCheckRequestDto request) {
		logger.info("Received request to create new healthcheck: {}", request.url());
		logger.debug("HealthCheck creation request details: {}", request);

		try {
			HealthCheckResponseDto createdHealthCheck = healthCheckService.createHealthCheck(request);
			auditLogger.info("HealthCheck created successfully - ID: {}, Name: {}", createdHealthCheck.id(),
					createdHealthCheck.url());
			logger.debug("Created healthcheck details: {}", createdHealthCheck);

			return ResponseEntity.status(HttpStatus.CREATED).body(createdHealthCheck);
		} 
		catch (NoSuchElementException e) {
			logger.error("Failed to create healthcheck: {} - Error: {}", request.url(), e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		catch (HealthCheckOperationException e) {
			logger.error("Failed to create healthcheck: {} - Error: {}", request.url(), e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<HealthCheckResponseDto> getHealthCheckById(@PathVariable Integer id) {
		logger.info("Fetching healthcheck by ID: {}", id);

		try {
			Optional<HealthCheckResponseDto> healthcheck = healthCheckService.findHealthCheckById(id);

			if (healthcheck.isPresent()) {
				logger.debug("Retrieved healthcheck details for ID {}: {}", id, healthcheck.get());
				return ResponseEntity.ok(healthcheck.get());
			} else {
				logger.warn("HealthCheck not found with ID: {}", id);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
		} catch (HealthCheckOperationException e) {
			logger.error("Error fetching healthcheck ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		} catch (HealthCheckNotFoundException e) {
			logger.error("HealthCheck not found with  ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping
	public ResponseEntity<List<HealthCheckResponseDto>> getAllHealthChecks() {
		logger.info("Fetching all healthchecks");

		try {
			CompletableFuture<List<HealthCheckResponseDto>> healthchecksFuture = healthCheckService.findAllHealthChecksAsync();
			List<HealthCheckResponseDto> healthchecks = healthchecksFuture.join();

			logger.debug("Retrieved {} healthchecks", healthchecks.size());
			return ResponseEntity.ok(healthchecks);
		} catch (Exception e) {
			logger.error("Error fetching all healthchecks: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<HealthCheckResponseDto> updateHealthCheck(@PathVariable Integer id,
			@RequestBody HealthCheckRequestDto request) {
		logger.info("Updating healthcheck ID: {}", id);
		logger.debug("Update details for healthcheck ID {}: {}", id, request);

		try {
			HealthCheckResponseDto updatedService = healthCheckService.updateHealthCheck(id, request);
			auditLogger.info("HealthCheck updated - ID: {}, Name: {}", id, updatedService.url());
			logger.debug("Updated healthcheck details: {}", updatedService);

			return ResponseEntity.ok(updatedService);
		} catch (HealthCheckOperationException e) {
			logger.error("Failed to update healthcheck ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteService(@PathVariable Integer id) {
		logger.info("Deleting healthcheck ID: {}", id);

		try {
			healthCheckService.deleteHealthCheckAsync(id);
			auditLogger.warn("HealthCheck deleted - ID: {}", id);
			return ResponseEntity.noContent().build();
		} catch (HealthCheckOperationException e) {
			logger.error("Failed to delete healthcheck ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
