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

import com.pawar.app.healthcheck.dto.HttpMethodsRequestDto;
import com.pawar.app.healthcheck.dto.HttpMethodsResponseDto;
import com.pawar.todo.amt.constants.HttpMethodName;
import com.pawar.todo.amt.exceptions.HttpMethodsOperationException;
import com.pawar.todo.amt.response.ApiResponse;
import com.pawar.todo.amt.service.HttpMethodsService;

/**
 * Controller for handling server-related operations
 */
@RestController
@RequestMapping("/api/http-methods")
public class HttpMethodsController {

    private static final Logger logger = LoggerFactory.getLogger(HttpMethodsController.class);
    private final HttpMethodsService httpMethodsService;

    public HttpMethodsController(HttpMethodsService httpMethodsService) {
        this.httpMethodsService = httpMethodsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<HttpMethodsResponseDto>>> getAllHttpMethods() {
        logger.debug("Entering getAllServers()");
        try {
            List<HttpMethodsResponseDto> httpMethods = httpMethodsService.findAllHttpMethodsAsync().get();
            logger.info("Successfully retrieved {} httpMethodsService", httpMethods.size());
            logger.debug("Exiting getAllHttpMethods() with {} httpMethodsService", httpMethods.size());
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "HttpMethods retrieved successfully",
                            httpMethods));
        } catch (HttpMessageNotReadableException e) {
            logger.error("Error retrieving httpMethods: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to retrieve httpMethods: " + e.getMessage(),
                            null));
        } catch (Exception e) {
            logger.error("Error retrieving httpMethods: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to retrieve httpMethods: " + e.getMessage(),
                            null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HttpMethodsResponseDto>> getServerById(@PathVariable Integer id) {
        logger.debug("Entering getServerById() with ID: {}", id);
        try {
            HttpMethodsResponseDto httpMethodsResponseDto = httpMethodsService.findHttpMethodById(id).get();
            logger.info("Successfully retrieved httpMethod with ID: {}", id);
            logger.debug("HttpMethod details: {}", httpMethodsResponseDto);
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "HttpMethods retrieved successfully",
                            httpMethodsResponseDto));
        }

        catch (Exception e) {
            logger.error("Error retrieving httpMethodsResponseDto with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to retrieve httpMethodsResponseDto: " + e.getMessage(),
                            null));
        }
    }

    @GetMapping("/method-name/{httpMethodName}")
    public ResponseEntity<ApiResponse<List<HttpMethodsResponseDto>>> getHttpMethodByMethodName(
            @PathVariable HttpMethodName httpMethodName) {
        logger.debug("Entering getHttpMethodByMethodName() with status: {}", httpMethodName);
        try {
            List<HttpMethodsResponseDto> httpMethodsResponseDtos = httpMethodsService
                    .findHttpMethodByMethodName(httpMethodName);
            logger.info("Found {} HttpMethods with status {}", httpMethodsResponseDtos.size(), httpMethodName);
            logger.debug("HttpMethods details: {}", httpMethodsResponseDtos);
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "HttpMethods retrieved successfully",
                            httpMethodsResponseDtos));
        } catch (Exception e) {
            logger.error("Error retrieving httpMethods with method name {}: {}", httpMethodName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to retrieve httpMethods: " + e.getMessage(),
                            null));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HttpMethodsResponseDto>> createHttpMethod(
            @RequestBody HttpMethodsRequestDto httpMethodsRequestDto) {
        logger.debug("Entering createHttpMethod() with request: {}", httpMethodsRequestDto);
        try {
            HttpMethodsResponseDto createdHttpMethod = httpMethodsService.createHttpMethod(httpMethodsRequestDto);
            logger.info("Successfully created httpMethod with ID: {}", createdHttpMethod.id());
            logger.debug("Created httpMethod details: {}", createdHttpMethod);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(
                            true,
                            "HttpMethod created successfully",
                            createdHttpMethod));
        } catch (HttpMethodsOperationException e) {
            logger.warn("Invalid create httpMethod request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(
                            false,
                            "Invalid request: " + e.getMessage(),
                            null));
        } catch (Exception e) {
            logger.error("Error creating httpMethod: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to create httpMethod: " + e.getMessage(),
                            null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HttpMethodsResponseDto>> updateHttpMethod(
            @PathVariable Integer id,
            @RequestBody HttpMethodsRequestDto httpMethodsRequestDto) {
        logger.debug("Entering updateHttpMethod() for ID {} with request: {}", id, httpMethodsRequestDto);
        try {
            HttpMethodsResponseDto updatedHttpMethod = httpMethodsService.updateHttpMethod(id, httpMethodsRequestDto);
            logger.info("Successfully updated httpMethod with ID: {}", id);
            logger.debug("Updated httpMethod details: {}", updatedHttpMethod);
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "HttpMethod updated successfully",
                            updatedHttpMethod));
        } catch (Exception e) {
            logger.error("Error updating httpMethod with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to update httpMethod: " + e.getMessage(),
                            null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHttpMethod(@PathVariable Integer id) {
        logger.debug("Entering deleteServer() for ID: {}", id);
        try {
            httpMethodsService.deleteHttpMethodAsync(id);
            logger.info("Successfully deleted httpMethod with ID: {}", id);
            return ResponseEntity.ok(
                    new ApiResponse<>(
                            true,
                            "HttpMethod deleted successfully",
                            null));
        } catch (HttpMethodsOperationException e) {
            logger.warn("HttpMethod not found for deletion with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(
                            false,
                            "HttpMethod not found with ID: " + id,
                            null));
        } catch (Exception e) {
            logger.error("Error deleting httpMethod with ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(
                            false,
                            "Failed to delete httpMethod: " + e.getMessage(),
                            null));
        }
    }
}