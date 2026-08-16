package com.pawar.todo.amt.controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawar.app.healthcheck.dto.ServiceHealthStatusRequestDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceHealthStatusOperationException;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.ServiceHealthStatusService;

/**
 * Controller for handling server-related operations
 */
@RestController
@RequestMapping("/api/service-health-status")
public class ServiceHealthStatusController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServiceHealthStatusController.class);
    private final ServiceHealthStatusService serviceHealthStatusService;
    
    public ServiceHealthStatusController(ServiceHealthStatusService serviceHealthStatusService) {
        this.serviceHealthStatusService = serviceHealthStatusService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceHealthStatusResponseDto>>> getAllServiceHealthStatuss() {
        logger.debug("Entering getAllServers()");
        try {
            List<ServiceHealthStatusResponseDto> serviceHealthStatuss = serviceHealthStatusService.findAllServiceHealthStatussAsync().get();
            logger.info("Successfully retrieved {} serviceHealthStatuss", serviceHealthStatuss.size());
            logger.debug("Exiting getAllServiceHealthStatuss() with {} serviceHealthStatuss", serviceHealthStatuss.size());
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "ServiceHealthStatuss retrieved successfully", 
                    serviceHealthStatuss
                )
            );
        } 
        catch (HttpMessageNotReadableException e) {
            logger.error("Error retrieving serviceHealthStatuss: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve serviceHealthStatuss: " + e.getMessage(), 
                    null
                ));
        }
        catch (HttpMessageNotWritableException e) {
        	e.printStackTrace();
            logger.error("Error retrieving serviceHealthStatuss: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve serviceHealthStatuss: " + e.getMessage(), 
                    null
                ));
        }
        catch (Exception e) {
        	
            logger.error("Error retrieving serviceHealthStatuss: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve serviceHealthStatuss: " + e.getMessage(), 
                    null
                ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Optional<ServiceHealthStatusResponseDto>>> getServiceHealthStatusById(@PathVariable Integer id) {
        logger.debug("Entering getServerById() with ID: {}", id);
        try {
            Optional<ServiceHealthStatusResponseDto> serviceHealthStatus = serviceHealthStatusService.findServiceHealthStatusById(id);
            logger.info("Successfully retrieved serviceHealthStatus with ID: {}", id);
            logger.debug("ServiceHealthStatus details: {}", serviceHealthStatus);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "ServiceHealthStatus retrieved successfully", 
                    serviceHealthStatus
                )
            );
        } 
//        catch (ResourceNotFoundException e) {
//            logger.warn("Server not found with ID: {}", id);
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                .body(new ApiResponse<>(
//                    false, 
//                    "Server not found with ID: " + id, 
//                    null
//                ));
//        } 
        
        catch (Exception e) {
            logger.error("Error retrieving serviceHealthStatus with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve serviceHealthStatus: " + e.getMessage(), 
                    null
                ));
        }
    }

    @GetMapping("/server/{id}")
    public ResponseEntity<ApiResponse<Optional<List<ServiceHealthStatusResponseDto>> >> getServiceHealthStatusByServerId(@PathVariable Integer id) {
        logger.debug("Entering getServerById() with ID: {}", id);
        try {
            Optional<List<ServiceHealthStatusResponseDto>>  serviceHealthStatus = serviceHealthStatusService.findServiceHealthStatusByServerId(id);
            logger.info("Successfully retrieved serviceHealthStatus with ID: {}", id);
            logger.debug("ServiceHealthStatus details: {}", serviceHealthStatus);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "ServiceHealthStatus retrieved successfully", 
                    serviceHealthStatus
                )
            );
        }
        catch (ResourceNotFoundException e) {
          logger.warn("serviceHealthStatus not found with ID: {}", id);
          return ResponseEntity.status(HttpStatus.NOT_FOUND)
              .body(new ApiResponse<>(
                  false, 
                  "serviceHealthStatus not found with ID: " + id, 
                  null
              ));
      } 
        catch (NoSuchElementException e) {
            logger.warn("serviceHealthStatus not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(
                    false, 
                    "serviceHealthStatus not found with ID: " + id, 
                    null
                ));
        } 
        catch (ServiceHealthStatusOperationException e) {
            logger.warn("serviceHealthStatus not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(
                    false, 
                    "serviceHealthStatus not found with ID: " + id, 
                    null
                ));
        } 
      
      catch (Exception e) {
          logger.error("Error retrieving serviceHealthStatus with ID {}: {}", id, e.getMessage(), e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(new ApiResponse<>(
                  false, 
                  "Failed to retrieve serviceHealthStatus: " + e.getMessage(), 
                  null
              ));
      }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ServiceHealthStatusResponseDto>>> getServiceHealthStatusByStatus(
            @PathVariable HealthCheckStatus status) {
        logger.debug("Entering getServersByStatus() with status: {}", status);
        try {
            List<ServiceHealthStatusResponseDto> serviceHealthStatusResponseDtos = serviceHealthStatusService.findServiceHealthStatusByStatus(status);
            logger.info("Found {} serviceHealthStatusResponseDtos with status {}", serviceHealthStatusResponseDtos.size(), status);
            logger.debug("ServiceHealthStatus details: {}", serviceHealthStatusResponseDtos);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "ServiceHealthStatuss retrieved successfully", 
                    serviceHealthStatusResponseDtos
                )
            );
        } catch (Exception e) {
            logger.error("Error retrieving serviceHealthStatusResponseDtos with status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve serviceHealthStatusResponseDtos: " + e.getMessage(), 
                    null
                ));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceHealthStatusResponseDto>> createServiceHealthStatus(
            @RequestBody ServiceHealthStatusRequestDto serviceHealthStatusRequest) {
        logger.debug("Entering createServiceHealthStatus() with request: {}", serviceHealthStatusRequest);
        try {
        	ServiceHealthStatusResponseDto createdServiceHealthStatus = serviceHealthStatusService.createServiceHealthStatus(serviceHealthStatusRequest);
            logger.info("Successfully created serviceHealthStatus with ID: {}", createdServiceHealthStatus.id());
            logger.debug("Created serviceHealthStatus details: {}", createdServiceHealthStatus);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                    true, 
                    "ServiceHealthStatus created successfully", 
                    createdServiceHealthStatus
                ));
        } catch (ServiceHealthStatusOperationException e) {
            logger.warn("Invalid create serviceHealthStatus request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                    false, 
                    "Invalid request: " + e.getMessage(), 
                    null
                ));
        } catch (Exception e) {
            logger.error("Error creating serviceHealthStatus: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to create serviceHealthStatus: " + e.getMessage(), 
                    null
                ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceHealthStatusResponseDto>> updateServiceHealthStatus(
            @PathVariable Integer id, 
            @RequestBody ServiceHealthStatusRequestDto serviceHealthStatusRequest) {
        logger.debug("Entering updateServiceHealthStatus() for ID {} with request: {}", id, serviceHealthStatusRequest);
        try {
        	ServiceHealthStatusResponseDto updatedServiceHealthStatus = serviceHealthStatusService.updateServiceHealthStatus(id, serviceHealthStatusRequest);
            logger.info("Successfully updated serviceHealthStatus with ID: {}", id);
            logger.debug("Updated serviceHealthStatus details: {}", updatedServiceHealthStatus);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "ServiceHealthStatus updated successfully", 
                    updatedServiceHealthStatus
                )
            );
        }  catch (Exception e) {
            logger.error("Error updating serviceHealthStatus with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to update serviceHealthStatus: " + e.getMessage(), 
                    null
                ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteServiceHealthStatus(@PathVariable Integer id) {
        logger.debug("Entering deleteServer() for ID: {}", id);
        try {
        	serviceHealthStatusService.deleteServiceHealthStatusAsync(id);
            logger.info("Successfully deleted serviceHealthStatus with ID: {}", id);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "ServiceHealthStatus deleted successfully", 
                    null
                )
            );
        } catch (ServiceHealthStatusOperationException e) {
            logger.warn("ServiceHealthStatus not found for deletion with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(
                    false, 
                    "ServiceHealthStatus not found with ID: " + id, 
                    null
                ));
        } catch (Exception e) {
            logger.error("Error deleting serviceHealthStatus with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to delete serviceHealthStatus: " + e.getMessage(), 
                    null
                ));
        }
    }

//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
//        logger.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//            .body(new ApiResponse<>(
//                false, 
//                "An unexpected error occurred: " + ex.getMessage(), 
//                null
//            ));
//    }
}
