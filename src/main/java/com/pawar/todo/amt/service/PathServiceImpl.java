package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.PathRequestDto;
import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.app.healthcheck.dto.ScriptRequestDto;

import com.pawar.todo.amt.cache.PathCache;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceAlreadyExistsException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.mapper.PathMapper;
import com.pawar.todo.amt.mapper.ScriptMapper;
import com.pawar.todo.amt.model.Path;
import com.pawar.todo.amt.model.Script;
import com.pawar.todo.amt.respository.PathRepository;

@Service
public class PathServiceImpl implements PathService {

	private final static Logger logger = LoggerFactory.getLogger(PathServiceImpl.class.getName());

	private final PathRepository pathRepository;
	private final PathMapper pathMapper;
	private final ScriptMapper scriptMapper;
	private final PathCache pathCache;

	public PathServiceImpl(PathRepository pathRepository, PathMapper pathMapper, ScriptMapper scriptMapper,
			PathCache pathCache) {
		this.pathRepository = pathRepository;
		this.pathMapper = pathMapper;
		this.scriptMapper = scriptMapper;
		this.pathCache = pathCache;
	}

	@Override
	@Transactional
	public PathResponseDto createPath(PathRequestDto pathRequestDto) {
		logger.info("Processing Path upsert: {}", pathRequestDto);

		LocalDateTime now = LocalDateTime.now();

		// Fetch existing entity or initialize a new one
		Path path = pathRepository.findByPathName(pathRequestDto.pathName())
				.orElseGet(() -> {
					Path newPath = new Path();
					newPath.setPathName(pathRequestDto.pathName());
					newPath.setCreatedDttm(now);
					return newPath;
				});

		// Map and assign scripts safely
		Set<Script> scripts = new HashSet<>();
		if (pathRequestDto.scripts() != null && !pathRequestDto.scripts().isEmpty()) {
			for (ScriptRequestDto scriptDto : pathRequestDto.scripts()) {
				scripts.add(scriptMapper.toEntity(scriptDto));
			}
		}
		path.setScripts(scripts);
		path.setLastUpdatedDttm(now);

		Path savedPath = pathRepository.save(path);
		logger.debug("Path saved successfully: ID={}", savedPath.getId());

		return pathMapper.toDto(savedPath);
	}

	@Override
	@Transactional
	public Optional<PathResponseDto> findPathById(Integer id) throws ResourceNotFoundException, PathOperationException {
		try {
			logger.debug("Fetching path by ID: {}", id);

			return pathCache.get(id).or(() -> {
				try {
					Path path = pathRepository.findById(id)
							.orElseThrow(() -> new ResourceNotFoundException("Server not found with ID: " + id));

					PathResponseDto dto = pathMapper.toDto(path);
					pathCache.put(id, dto);
					return Optional.of(dto);
				} catch (ResourceNotFoundException e) {
					logger.error("Error fetching server with ID: {}", id, e);
					return Optional.empty(); // Return empty if not found
				}
			});
		} catch (Exception e) {
			logger.error("Error fetching server with ID: {}", id, e);
			throw new PathOperationException("Failed to fetch server", e);
		}
	}

	@Async
	@Transactional(readOnly = true)
	@Override
	public CompletableFuture<List<PathResponseDto>> findAllPathsAsync() throws ResourceNotFoundException {
		logger.info("Async fetching of all paths initiated");

		return CompletableFuture.supplyAsync(() -> {
			try {
				List<Path> paths = pathRepository.findAll();

				logger.info("Path : {}", paths);

				// List<PathResponseDto> response = paths.stream()
				// .peek(path -> logger.info("Processing path : {}",
				// path.toString())).map(pathMapper::toDto)
				// .collect(Collectors.toList());
				List<PathResponseDto> pathResponseDtos = new ArrayList<>();
				for (Path path : paths) {
					PathResponseDto pathResponseDto = pathMapper.toDto(path);
					pathResponseDtos.add(pathResponseDto);
				}

				logger.info("Async paths fetch completed successfully");
				return pathResponseDtos;
			} catch (Exception e) {
				logger.error("Async paths fetch failed", e);
				throw new CompletionException("Failed to fetch paths asynchronously", e);
			}
		});
	}

	@Override
	@Transactional
	public PathResponseDto updatePath(Integer id, PathRequestDto pathRequestDto) throws PathOperationException {
		try {
			logger.info("Updating path ID: {}", id);

			Path path = pathRepository.findById(id)
					.orElseThrow(() -> new ResourceNotFoundException("Path not found with ID: " + id));

			Set<ScriptRequestDto> scriptRequestDtos = pathRequestDto.scripts();
			Set<Script> scripts = new HashSet<>();

			if (!scriptRequestDtos.isEmpty()) {
				for (ScriptRequestDto scriptRequestDto : scriptRequestDtos) {
					Script script = scriptMapper.toEntity(scriptRequestDto);
					scripts.add(script);
				}
				path.setScripts(scripts);
			}

			pathMapper.updateFromDto(pathRequestDto, path);
			path.setLastUpdatedDttm(LocalDateTime.now());

			Path updatedPath = pathRepository.save(path);
			pathCache.evict(id);
			logger.debug("Path updated successfully: ID={}", id);

			return pathMapper.toDto(updatedPath);
		} catch (Exception e) {
			logger.error("Failed to update path ID: {}", id, e);
			throw new PathOperationException("Failed to update path", e);
		}
	}

	@Override
	@Transactional
	public ListenableFuture<Void> deletePathAsync(Integer id) throws PathOperationException {
		try {
			logger.info("Async deletion initiated for path ID: {}", id);

			pathRepository.deleteById(id);
			pathCache.evict(id);
			logger.debug("Async deletion completed for path ID: {}", id);

			return new AsyncResult<>(null);
		} catch (Exception e) {
			logger.error("Async deletion failed for path ID: {}", id, e);
			throw new PathOperationException("Failed to delete path asynchronously", e);
		}
	}

	private boolean validatePathRequest(PathRequestDto dto) throws ResourceAlreadyExistsException {
		return pathRepository.existsByPathName(dto.pathName());
	}

	@Override
	public Optional<PathResponseDto> findByPathName(String pathName) throws PathOperationException {
		try {
			logger.debug("Fetching path by pathName: {}", pathName);

			return pathCache.get(pathName).or(() -> {
				try {
					Path path = pathRepository.findByPathName(pathName)
							.orElseThrow(() -> new ResourceNotFoundException(
									"Path not found with pathName: " + pathName));

					PathResponseDto dto = pathMapper.toDto(path);
					pathCache.put(pathName, dto);
					return Optional.of(dto);
				} catch (ResourceNotFoundException e) {
					logger.error("Error fetching path with pathName: {}", pathName, e);
					return Optional.empty(); // Return empty if not found
				}
			});
		} catch (Exception e) {
			logger.error("Error fetching path with ID: {}", pathName, e);
			throw new PathOperationException("Failed to fetch path", e);
		}
	}

	@Override
	public Optional<PathRequestDto> findPathByPathId(Integer id)
			throws ResourceNotFoundException, PathOperationException {
		try {
			logger.debug("Fetching path by ID: {}", id);

			return pathCache.gett(id).or(() -> {
				try {
					Path path = pathRepository.findById(id)
							.orElseThrow(() -> new ResourceNotFoundException("Server not found with ID: " + id));

					PathRequestDto dto = pathMapper.reqToDto(path);
					pathCache.put(id, dto);
					return Optional.of(dto);
				} catch (ResourceNotFoundException e) {
					logger.error("Error fetching server with ID: {}", id, e);
					return Optional.empty(); // Return empty if not found
				}
			});
		} catch (Exception e) {
			logger.error("Error fetching server with ID: {}", id, e);
			throw new PathOperationException("Failed to fetch server", e);
		}
	}

}
