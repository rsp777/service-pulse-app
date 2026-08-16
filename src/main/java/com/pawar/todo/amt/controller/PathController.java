package com.pawar.todo.amt.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.InvalidRequestException;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceAlreadyExistsException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.PathService;
import com.pawar.todo.amt.service.ServerService;

/**
 * Controller for handling server-related operations
 */
@RestController
@RequestMapping("/api/paths")
public class PathController {
    
    private static final Logger logger = LoggerFactory.getLogger(PathController.class);
    private final PathService pathService;
    
    public PathController(PathService pathService) {
        this.pathService = pathService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PathResponseDto>>> getAllPaths() {
        logger.debug("Entering getAllServers()");
        try {
            List<PathResponseDto> paths = pathService.findAllPathsAsync().get();
            logger.info("Successfully retrieved {} paths", paths.size());
            logger.debug("Exiting getAllPaths() with {} paths", paths.size());
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Paths retrieved successfully", 
                    paths
                )
            );
        } 
        catch (HttpMessageNotReadableException e) {
        	logger.error("Error retrieving paths: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve paths: " + e.getMessage(), 
                    null
                ));
        }
        catch (Exception e) {
            logger.error("Error retrieving paths: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve paths: " + e.getMessage(), 
                    null
                ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PathResponseDto>> getPathById(@PathVariable Integer id) {
        logger.debug("Entering getPathById() with ID: {}", id);
        try {
        	PathResponseDto pathResponseDto = pathService.findPathById(id).get();
            logger.info("Successfully retrieved path with ID: {}", id);
            logger.debug("path details: {}", pathResponseDto);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Path retrieved successfully", 
                    pathResponseDto
                )
            );
        } 
        catch (ResourceNotFoundException e) {
            logger.warn("Path not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(
                    false, 
                    "Path not found with ID: " + id, 
                    null
                ));
        } 
        
        catch (Exception e) {
            logger.error("Error retrieving path with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve path: " + e.getMessage(), 
                    null
                ));
        }
    }

//    @GetMapping("/status/{status}")
//    public ResponseEntity<ApiResponse<List<PathResponseDto>>> getServersByStatus(
//            @PathVariable ServerStatus status) {
//        logger.debug("Entering getServersByStatus() with status: {}", status);
//        try {
//            List<PathResponseDto> servers = serverService.findServersByStatus(status);
//            logger.info("Found {} servers with status {}", servers.size(), status);
//            logger.debug("Server details: {}", servers);
//            return ResponseEntity.ok(
//                new ApiResponse<>(
//                    true, 
//                    "Servers retrieved successfully", 
//                    servers
//                )
//            );
//        } catch (Exception e) {
//            logger.error("Error retrieving servers with status {}: {}", status, e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(new ApiResponse<>(
//                    false, 
//                    "Failed to retrieve servers: " + e.getMessage(), 
//                    null
//                ));
//        }
//    }

    @PostMapping
    public ResponseEntity<ApiResponse<PathResponseDto>> createPath(
            @RequestBody PathRequestDto pathRequest) {
        logger.debug("Entering createPath() with request: {}", pathRequest);
        try {
        	PathResponseDto createPath = pathService.createPath(pathRequest);
            logger.info("Successfully created path with ID: {}", createPath.id());
            logger.debug("Created path details: {}", createPath);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                    true, 
                    "Path created successfully", 
                    createPath
                ));
        } catch (ResourceAlreadyExistsException e) {
            logger.warn("Invalid create path request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                    false, 
                    "Invalid request: " + e.getMessage(), 
                    null
                ));
        } catch (Exception e) {
            logger.error("Error creating path: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to create path: " + e.getMessage(), 
                    null
                ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PathResponseDto>> updatePath(
            @PathVariable Integer id, 
            @RequestBody PathRequestDto pathRequest) {
        logger.debug("Entering updatePath() for ID {} with request: {}", id, pathRequest);
        try {
        	PathResponseDto updatedPath = pathService.updatePath(id, pathRequest);
            logger.info("Successfully updated path with ID: {}", id);
            logger.debug("Updated path details: {}", updatedPath);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Path updated successfully", 
                    updatedPath
                )
            );
        }  catch (Exception e) {
            logger.error("Error updating path with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to update path: " + e.getMessage(), 
                    null
                ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePath(@PathVariable Integer id) {
        logger.debug("Entering deletePath() for ID: {}", id);
        try {
            pathService.deletePathAsync(id);
            logger.info("Successfully deleted path with ID: {}", id);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Path deleted successfully", 
                    null
                )
            );
        } catch (PathOperationException e) {
            logger.warn("Path not found for deletion with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(
                    false, 
                    "Path not found with ID: " + id, 
                    null
                ));
        } catch (Exception e) {
            logger.error("Error deleting path with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to delete path: " + e.getMessage(), 
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
