package com.pawar.todo.amt.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.exceptions.ServiceOperationException;
import com.pawar.todo.amt.service.ServiceService;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

	private static final Logger logger = LoggerFactory.getLogger(ServiceController.class);
	private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

	private final ServiceService serviceService;

	public ServiceController(ServiceService serviceService) {
		this.serviceService = serviceService;
	}

	@PostMapping
	public ResponseEntity<ServiceResponseDto> createService(@RequestBody ServiceRequestDto request) {
		logger.info("Received request to create new service: {}", request.serviceName());
		logger.debug("Service creation request details: {}", request);

		try {
			ServiceResponseDto createdService = serviceService.createService(request);
			auditLogger.info("Service created successfully - ID: {}, Name: {}", createdService.id(),
					createdService.serviceName());
			logger.debug("Created service details: {}", createdService);

			return ResponseEntity.status(HttpStatus.CREATED).body(createdService);
		} catch (ServiceOperationException e) {
			logger.error("Failed to create service: {} - Error: {}", request.serviceName(), e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.error("Failed to create service: {} - Error: {}", request.serviceName(), e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping("/{id}")
	public ResponseEntity<ServiceResponseDto> getServiceById(@PathVariable Integer id) {
		logger.info("Fetching service by ID: {}", id);

		try {
			Optional<ServiceResponseDto> service = serviceService.findServiceById(id);

			if (service.isPresent()) {
				logger.debug("Retrieved service details for ID {}: {}", id, service.get());
				return ResponseEntity.ok(service.get());
			} else {
				logger.warn("Service not found with ID: {}", id);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
		} catch (ServiceOperationException e) {
			logger.error("Error fetching service ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping
	public ResponseEntity<List<ServiceResponseDto>> getAllServices() {
		logger.info("Fetching all services");

		try {
			CompletableFuture<List<ServiceResponseDto>> servicesFuture = serviceService.findAllServicesAsync();
			List<ServiceResponseDto> services = servicesFuture.join();

			logger.debug("Retrieved {} services", services.size());
			return ResponseEntity.ok(services);
		} catch (Exception e) {
			logger.error("Error fetching all services: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PutMapping("/{id}")
	public ResponseEntity<ServiceResponseDto> updateService(@PathVariable Integer id,
			@RequestBody ServiceRequestDto request) {
		logger.info("Updating service ID: {}", id);
		logger.debug("Update details for service ID {}: {}", id, request);

		try {
			ServiceResponseDto updatedService = serviceService.updateService(id, request);
			auditLogger.info("Service updated - ID: {}, Name: {}", id, updatedService.serviceName());
			logger.debug("Updated service details: {}", updatedService);

			return ResponseEntity.ok(updatedService);
		} catch (ServiceOperationException e) {
			logger.error("Failed to update service ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteService(@PathVariable Integer id) {
		logger.info("Deleting service ID: {}", id);

		try {
			serviceService.deleteServiceAsync(id);
			auditLogger.warn("Service deleted - ID: {}", id);
			return ResponseEntity.noContent().build();
		} catch (ServiceOperationException e) {
			logger.error("Failed to delete service ID {}: {}", id, e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}
}
