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

import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.InvalidRequestException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.ServerService;

/**
 * Controller for handling server-related operations
 */
@RestController
@RequestMapping("/api/servers")
public class ServerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerController.class);
    private final ServerService serverService;
    
    public ServerController(ServerService serverService) {
        this.serverService = serverService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServerResponseDto>>> getAllServers() {
        logger.debug("Entering getAllServers()");
        try {
            List<ServerResponseDto> servers = serverService.findAllServersAsync().get();
            logger.info("Successfully retrieved {} servers", servers.size());
            logger.debug("Exiting getAllServers() with {} servers", servers.size());
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Servers retrieved successfully", 
                    servers
                )
            );
        } 
        catch (HttpMessageNotReadableException e) {
        	logger.error("Error retrieving servers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve servers: " + e.getMessage(), 
                    null
                ));
        }
        
        catch (Throwable e) {
            logger.error("Error retrieving servers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve servers: " + e.getMessage(), 
                    null
                ));
        }
        
        
//        catch (Exception e) {
//        	e.printStackTrace();
//            logger.error("Error retrieving servers: {}", e.getMessage(), e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                .body(new ApiResponse<>(
//                    false, 
//                    "Failed to retrieve servers: " + e.getMessage(), 
//                    null
//                ));
//        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServerResponseDto>> getServerById(@PathVariable Integer id) {
        logger.debug("Entering getServerById() with ID: {}", id);
        try {
            ServerResponseDto server = serverService.findServerById(id).get();
            logger.info("Successfully retrieved server with ID: {}", id);
            logger.debug("Server details: {}", server);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Server retrieved successfully", 
                    server
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
            logger.error("Error retrieving server with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve server: " + e.getMessage(), 
                    null
                ));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ServerResponseDto>>> getServersByStatus(
            @PathVariable ServerStatus status) {
        logger.debug("Entering getServersByStatus() with status: {}", status);
        try {
            List<ServerResponseDto> servers = serverService.findServersByStatus(status);
            logger.info("Found {} servers with status {}", servers.size(), status);
            logger.debug("Server details: {}", servers);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Servers retrieved successfully", 
                    servers
                )
            );
        } catch (Exception e) {
            logger.error("Error retrieving servers with status {}: {}", status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve servers: " + e.getMessage(), 
                    null
                ));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServerResponseDto>> createServer(
            @RequestBody ServerRequestDto serverRequest) {
        logger.debug("Entering createServer() with request: {}", serverRequest);
        try {
            ServerResponseDto createdServer = serverService.createServer(serverRequest);
            logger.info("Successfully created server with ID: {}", createdServer.id());
            logger.debug("Created server details: {}", createdServer);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                    true, 
                    "Server created successfully", 
                    createdServer
                ));
        } catch (ServerOperationException e) {
            logger.warn("Invalid create server request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                    false, 
                    "Invalid request: " + e.getMessage(), 
                    null
                ));
        } catch (Exception e) {
            logger.error("Error creating server: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to create server: " + e.getMessage(), 
                    null
                ));
        }
        catch (Throwable e) {
            logger.error("Error creating server: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to create server: " + e.getMessage(), 
                    null
                ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServerResponseDto>> updateServer(
            @PathVariable Integer id, 
            @RequestBody ServerRequestDto serverRequest) {
        logger.debug("Entering updateServer() for ID {} with request: {}", id, serverRequest);
        try {
            ServerResponseDto updatedServer = serverService.updateServer(id, serverRequest);
            logger.info("Successfully updated server with ID: {}", id);
            logger.debug("Updated server details: {}", updatedServer);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Server updated successfully", 
                    updatedServer
                )
            );
        }  catch (Exception e) {
            logger.error("Error updating server with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to update server: " + e.getMessage(), 
                    null
                ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteServer(@PathVariable Integer id) {
        logger.debug("Entering deleteServer() for ID: {}", id);
        try {
            serverService.deleteServerAsync(id);
            logger.info("Successfully deleted server with ID: {}", id);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Server deleted successfully", 
                    null
                )
            );
        } catch (ServerOperationException e) {
            logger.warn("Server not found for deletion with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(
                    false, 
                    "Server not found with ID: " + id, 
                    null
                ));
        } catch (Exception e) {
            logger.error("Error deleting server with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to delete server: " + e.getMessage(), 
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
