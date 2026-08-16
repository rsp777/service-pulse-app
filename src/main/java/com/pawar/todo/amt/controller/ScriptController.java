package com.pawar.todo.amt.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pawar.app.healthcheck.dto.ScriptRequestDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.todo.amt.exceptions.ResourceAlreadyExistsException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ScriptOperationException;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.ScriptService;

/**
 * Controller for handling server-related operations
 */
@RestController
@RequestMapping("/api/scripts")
public class ScriptController {
    
    private static final Logger logger = LoggerFactory.getLogger(ScriptController.class);
    private final ScriptService scriptService;
    
    public ScriptController(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScriptResponseDto>>> getAllScripts() {
        logger.debug("Entering getAllServers()");
        try {
            List<ScriptResponseDto> scripts = scriptService.findAllScriptsAsync().get();
            logger.info("Successfully retrieved {} scripts", scripts.size());
            logger.debug("Exiting getAllPaths() with {} scripts", scripts.size());
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Scripts retrieved successfully", 
                    scripts
                )
            );
        } 
        catch (HttpMessageNotReadableException e) {
        	logger.error("Error retrieving scripts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve scripts: " + e.getMessage(), 
                    null
                ));
        }
        catch (Exception e) {
            logger.error("Error retrieving scripts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve scripts: " + e.getMessage(), 
                    null
                ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ScriptResponseDto>> getScriptById(@PathVariable Integer id) {
        logger.debug("Entering getScriptById() with ID: {}", id);
        try {
        	ScriptResponseDto ScriptResponseDto = scriptService.findScriptById(id).get();
            logger.info("Successfully retrieved script with ID: {}", id);
            logger.debug("script details: {}", ScriptResponseDto);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Script retrieved successfully", 
                    ScriptResponseDto
                )
            );
        } 
        catch (ResourceNotFoundException e) {
            logger.warn("Script not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(
                    false, 
                    "Script not found with ID: " + id, 
                    null
                ));
        } 
        
        catch (Exception e) {
            logger.error("Error retrieving script with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to retrieve script: " + e.getMessage(), 
                    null
                ));
        }
    }

//    @GetMapping("/status/{status}")
//    public ResponseEntity<ApiResponse<List<ScriptResponseDto>>> getServersByStatus(
//            @PathVariable ServerStatus status) {
//        logger.debug("Entering getServersByStatus() with status: {}", status);
//        try {
//            List<ScriptResponseDto> servers = serverService.findServersByStatus(status);
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
    public ResponseEntity<ApiResponse<ScriptResponseDto>> createScript(
            @RequestBody ScriptRequestDto scriptRequest) {
        logger.debug("Entering createScript() with request: {}", scriptRequest);
        try {
        	ScriptResponseDto createdScript = scriptService.createScript(scriptRequest);
            logger.info("Successfully created script with ID: {}", createdScript.id());
            logger.debug("Created script details: {}", createdScript);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(
                    true, 
                    "Script created successfully", 
                    createdScript
                ));
        } catch (ResourceAlreadyExistsException e) {
            logger.warn("Invalid create path script: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(
                    false, 
                    "Invalid request: " + e.getMessage(), 
                    null
                ));
        } catch (Exception e) {
            logger.error("Error creating script: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to create path: " + e.getMessage(), 
                    null
                ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScriptResponseDto>> updateScript(
            @PathVariable Integer id, 
            @RequestBody ScriptRequestDto scriptRequest) {
        logger.debug("Entering updatePath() for ID {} with request: {}", id, scriptRequest);
        try {
        	ScriptResponseDto updatedScript = scriptService.updateScript(id, scriptRequest);
            logger.info("Successfully updated script with ID: {}", id);
            logger.debug("Updated script details: {}", updatedScript);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Script updated successfully", 
                    updatedScript
                )
            );
        }  catch (Exception e) {
            logger.error("Error updating script with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to update script: " + e.getMessage(), 
                    null
                ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteScript(@PathVariable Integer id) {
        logger.debug("Entering deleteScript() for ID: {}", id);
        try {
            scriptService.deleteScriptAsync(id);
            logger.info("Successfully deleted script with ID: {}", id);
            return ResponseEntity.ok(
                new ApiResponse<>(
                    true, 
                    "Script deleted successfully", 
                    null
                )
            );
        } catch (ScriptOperationException e) {
            logger.warn("Script not found for deletion with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(
                    false, 
                    "Script not found with ID: " + id, 
                    null
                ));
        } catch (Exception e) {
            logger.error("Error deleting script with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(
                    false, 
                    "Failed to delete script: " + e.getMessage(), 
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
