package com.pawar.todo.amt.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.ServiceRequestDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.exceptions.ServiceOperationException;

public interface ServiceService {
	/**
     * Creates a new service.
     *
     * @param request the service request data
     * @return the created service response DTO
     * @throws ServiceOperationException if an error occurs during service creation
     */
    ServiceResponseDto createService(ServiceRequestDto request) throws ServiceOperationException;

    /**
     * Finds a service by its ID.
     *
     * @param id the ID of the service
     * @return an Optional containing the service response DTO if found, or empty if not found
     * @throws ServiceOperationException if an error occurs during the fetch operation
     */
    Optional<ServiceResponseDto> findServiceById(Integer id) throws ServiceOperationException;

    /**
     * Asynchronously finds all services.
     *
     * @return a CompletableFuture containing a list of service response DTOs
     */
    CompletableFuture<List<ServiceResponseDto>> findAllServicesAsync();

    /**
     * Updates an existing service.
     *
     * @param id the ID of the service to update
     * @param request the service request data with updated values
     * @return the updated service response DTO
     * @throws ServiceOperationException if an error occurs during the update operation
     */
    ServiceResponseDto updateService(Integer id, ServiceRequestDto request) throws ServiceOperationException;

    /**
     * Asynchronously deletes a service by its ID.
     *
     * @param id the ID of the service to delete
     * @return a ListenableFuture indicating the completion of the deletion
     * @throws ServiceOperationException if an error occurs during the deletion operation
     */
    ListenableFuture<Void> deleteServiceAsync(Integer id) throws ServiceOperationException;
    
    /**
     * Checks if a service exists by its ID.
     *
     * @param serviceName the serviceName of the service to check
     * @return true if the service exists, false otherwise
     * @throws ServiceOperationException if an error occurs during the existence check
     */
    boolean isServiceExist(String serviceName) throws ServiceOperationException;

}