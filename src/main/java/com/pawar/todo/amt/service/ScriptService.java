package com.pawar.todo.amt.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.ScriptRequestDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.todo.amt.exceptions.ResourceAlreadyExistsException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ScriptOperationException;

public interface ScriptService {
	/**
     * Creates a new scripts.
     *
     * @param request the script request data
     * @return the created script response DTO
     * @throws ResourceAlreadyExistsException if an error occurs during script creation
     */
    ScriptResponseDto createScript(ScriptRequestDto request) throws ResourceAlreadyExistsException;

    /**
     * Finds a script by its ID.
     *
     * @param id the ID of the script
     * @return an Optional containing the script response DTO if found, or empty if not found
     * @throws ResourceNotFoundException if an error occurs during the fetch operation
     */
    Optional<ScriptResponseDto> findScriptById(Integer id) throws ResourceNotFoundException;

    /**
     * Asynchronously finds all scripts.
     *
     * @return a CompletableFuture containing a list of script response DTOs
     */
    CompletableFuture<List<ScriptResponseDto>> findAllScriptsAsync()throws ResourceNotFoundException;

    /**
     * Updates an existing script.
     *
     * @param id the ID of the script to update
     * @param request the service request data with updated values
     * @return the updated script response DTO
     * @throws ScriptOperationException if an error occurs during the update operation
     */
    ScriptResponseDto updateScript(Integer id, ScriptRequestDto request) throws ScriptOperationException;

    /**
     * Asynchronously deletes a script by its ID.
     *
     * @param id the ID of the script to delete
     * @return a ListenableFuture indicating the completion of the deletion
     * @throws ScriptOperationException if an error occurs during the deletion operation
     */
    ListenableFuture<Void> deleteScriptAsync(Integer id) throws ScriptOperationException;
    
    /**
     * Finds a script by its name.
     *
     * @param id the ID of the script
     * @return an Optional containing the script response DTO if found, or empty if not found
     * @throws ResourceNotFoundException if an error occurs during the fetch operation
     */
    Optional<ScriptResponseDto> findScriptByScriptName(String name) throws ResourceNotFoundException;
}