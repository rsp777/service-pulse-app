package com.pawar.todo.amt.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceAlreadyExistsException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;

public interface PathService {
	/**
     * Creates a new service.
     *
     * @param request the service request data
     * @return the created service response DTO
     * @throws ResourceAlreadyExistsException if an error occurs during service creation
     */
    PathResponseDto createPath(PathRequestDto request) throws ResourceAlreadyExistsException;

    /**
     * Finds a service by its ID.
     *
     * @param id the ID of the service
     * @return an Optional containing the service response DTO if found, or empty if not found
     * @throws ResourceNotFoundException if an error occurs during the fetch operation
     * @throws PathOperationException 
     */
    Optional<PathResponseDto> findPathById(Integer id) throws ResourceNotFoundException, PathOperationException;
    
    /**
     * Finds a service by its ID.
     *
     * @param id the ID of the service
     * @return an Optional containing the service response DTO if found, or empty if not found
     * @throws ResourceNotFoundException if an error occurs during the fetch operation
     * @throws PathOperationException 
     */
    Optional<PathRequestDto> findPathByPathId(Integer id) throws ResourceNotFoundException, PathOperationException;

    /**
     * Asynchronously finds all services.
     *
     * @return a CompletableFuture containing a list of service response DTOs
     */
    CompletableFuture<List<PathResponseDto>> findAllPathsAsync() throws ResourceNotFoundException;

    /**
     * Updates an existing service.
     *
     * @param id the ID of the service to update
     * @param request the service request data with updated values
     * @return the updated service response DTO
     * @throws PathOperationException if an error occurs during the update operation
     */
    PathResponseDto updatePath(Integer id, PathRequestDto request) throws PathOperationException;

    /**
     * Asynchronously deletes a service by its ID.
     *
     * @param id the ID of the service to delete
     * @return a ListenableFuture indicating the completion of the deletion
     * @throws PathOperationException if an error occurs during the deletion operation
     */
    ListenableFuture<Void> deletePathAsync(Integer id) throws PathOperationException;

    
    /**
     * Finds a service by its Path Name.
     *
     * @param id the Path Name of the service
     * @return an Optional containing the service response DTO if found, or empty if not found
     * @throws ResourceNotFoundException if an error occurs during the fetch operation
     * @throws PathOperationException 
     */
    Optional<PathResponseDto> findByPathName(String description) throws PathOperationException;	


    
}