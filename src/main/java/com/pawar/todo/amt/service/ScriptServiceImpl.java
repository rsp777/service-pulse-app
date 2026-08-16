package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.app.healthcheck.dto.ScriptRequestDto;
import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.cache.ScriptCache;
import com.pawar.todo.amt.cache.ServerCache;
import com.pawar.todo.amt.exceptions.ResourceAlreadyExistsException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ScriptOperationException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.mapper.ScriptMapper;
import com.pawar.todo.amt.mapper.ServerMapper;
import com.pawar.todo.amt.model.Script;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.respository.ScriptRepository;
import com.pawar.todo.amt.respository.ServerRepository;
import com.pawar.todo.amt.validator.ServerStatusValidator;

@Service
public class ScriptServiceImpl implements ScriptService {

	private final static Logger logger = LoggerFactory.getLogger(ScriptServiceImpl.class.getName());

	private final ScriptRepository scriptRepository;
	private ScriptMapper scriptMapper;
	private final ScriptCache scriptCache;

	public ScriptServiceImpl(ScriptRepository scriptRepository,ScriptCache scriptCache) {
		this.scriptRepository = scriptRepository;
		this.scriptCache = scriptCache;
	}

	@Autowired
	  public void setScriptMapper(ScriptMapper scriptMapper) {
		this.scriptMapper = scriptMapper;
	}

	@Override
	@Transactional
	public ScriptResponseDto createScript(ScriptRequestDto scriptRequestDto) throws ResourceAlreadyExistsException {
		try {
			logger.info("Creating new script: {}", scriptRequestDto.scriptName());
			validateScriptRequest(scriptRequestDto);

			Script script = scriptMapper.toEntity(scriptRequestDto);
			script.setCreatedDttm(LocalDateTime.now());
			script.setLastUpdatedDttm(LocalDateTime.now());

			Script savedScript = scriptRepository.save(script);
			logger.debug("Script created successfully: ID={}", savedScript.getId());

			return scriptMapper.toDto(savedScript);
		} catch (Exception e) {
			logger.error("Failed to create script: ", e);
			throw new ResourceAlreadyExistsException("Failed to create script", e);
		}
	}

	@Override
	@Transactional
	public Optional<ScriptResponseDto> findScriptById(Integer id) throws ResourceNotFoundException {
		try {
			logger.debug("Fetching script by ID: {}", id);

			return scriptCache.get(id).or(() -> {
				try {
					Script script = scriptRepository.findById(id)
							.orElseThrow(() -> new ResourceNotFoundException("Server not found with ID: " + id));

					ScriptResponseDto dto = scriptMapper.toDto(script);
					scriptCache.put(id, dto);
					return Optional.of(dto);
				} catch (ResourceNotFoundException e) {
					logger.error("Error fetching script with ID: {}", id, e);
					return Optional.empty(); // Return empty if not found
				}
			});
		} catch (Exception e) {
			logger.error("Error fetching script with ID: {}", id, e);
			throw new ResourceNotFoundException("Failed to fetch script", e);
		}
	}

	@Override
	@Transactional
	public Optional<ScriptResponseDto> findScriptByScriptName(String name) throws ResourceNotFoundException {
		try {
			logger.debug("Fetching script by name: {}", name);

			return scriptCache.get(name).or(() -> {
				try {
					Script script = scriptRepository.findByScriptName(name)
							.orElseThrow(() -> new ResourceNotFoundException("Script not found with name: " + name));

					ScriptResponseDto dto = scriptMapper.toDto(script);
					scriptCache.put(dto.scriptName(), dto);
					return Optional.of(dto);
				} catch (ResourceNotFoundException e) {
					logger.error("Error fetching script with name: {}", name, e);
					return Optional.empty(); // Return empty if not found
				}
			});
		} catch (Exception e) {
			logger.error("Error fetching script with name: {}", name, e);
			throw new ResourceNotFoundException("Failed to fetch script", e);
		}
	}

	@Override
	@Async
	@Transactional(readOnly = true)
	public CompletableFuture<List<ScriptResponseDto>> findAllScriptsAsync() throws ResourceNotFoundException {
		try {
			logger.info("Async fetching of all scripts initiated");

			List<Script> scripts = scriptRepository.findAll();
			List<ScriptResponseDto> response = scripts.stream()
					.peek(s -> logger.debug("Processing script: {}", s.getId())).map(scriptMapper::toDto).toList();

			logger.info("Async script fetch completed successfully");
			return CompletableFuture.completedFuture(response);
		} catch (Exception e) {
			logger.error("Async server fetch failed", e);
			throw new CompletionException("Failed to fetch scripts asynchronously", e);
		}
	}

	@Override
	@Transactional
	public ScriptResponseDto updateScript(Integer id, ScriptRequestDto scriptRequestDto)
			throws ScriptOperationException {
		try {
			logger.info("Updating script ID: {}", id);

			Script script = scriptRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Script not found with ID: " + id));

			scriptMapper.updateFromDto(scriptRequestDto, script);
			script.setLastUpdatedDttm(LocalDateTime.now());

			Script updatedScript = scriptRepository.save(script);
			scriptCache.evict(id);
			logger.debug("Script updated successfully: ID={}", id);

			return scriptMapper.toDto(updatedScript);
		} catch (Exception e) {
			logger.error("Failed to update script ID: {}", id, e);
			throw new ScriptOperationException("Failed to update script", e);
		}
	}

	@Override
	@Async
	@Transactional
	public ListenableFuture<Void> deleteScriptAsync(Integer id) throws ScriptOperationException {
		try {
			logger.info("Async deletion initiated for script ID: {}", id);

			scriptRepository.deleteById(id);
			scriptCache.evict(id);
			logger.debug("Async deletion completed for script ID: {}", id);

			return new AsyncResult<>(null);
		} catch (Exception e) {
			logger.error("Async deletion failed for script ID: {}", id, e);
			throw new ScriptOperationException("Failed to delete script asynchronously", e);
		}
	}

	private void validateScriptRequest(ScriptRequestDto dto) throws ResourceAlreadyExistsException {

		if (scriptRepository.existsByScriptName(dto.scriptName())) {
			throw new ResourceAlreadyExistsException(
					"Script with scriptName '" + dto.scriptName() + "' already exists");
		}

	}

}
