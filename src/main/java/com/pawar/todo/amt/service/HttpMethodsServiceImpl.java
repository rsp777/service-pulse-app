package com.pawar.todo.amt.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.HttpMethodsRequestDto;
import com.pawar.app.healthcheck.dto.HttpMethodsResponseDto;
import com.pawar.todo.amt.cache.HttpMethodsCache;
import com.pawar.todo.amt.constants.HttpMethodName;
import com.pawar.todo.amt.converter.HttpMethodNameConverter;
import com.pawar.todo.amt.exceptions.HttpMethodsOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.mapper.HttpMethodsMapper;
import com.pawar.todo.amt.model.HttpMethods;
import com.pawar.todo.amt.respository.HttpMethodsRepository;


@Service
public class HttpMethodsServiceImpl implements HttpMethodsService {

    private final static Logger logger = LoggerFactory.getLogger(HttpMethodsServiceImpl.class.getName());

    private final HttpMethodsRepository httpMethodsRepository;
    private final HttpMethodsMapper httpMethodsMapper;
    private final HttpMethodsCache httpMethodsCache ;
    private final HttpMethodNameConverter httpMethodNameConverter;
    
    public HttpMethodsServiceImpl(HttpMethodsRepository httpMethodsRepository,
    							  HttpMethodsMapper httpMethodsMapper,
    							  HttpMethodsCache httpMethodsCache,
    							  HttpMethodNameConverter httpMethodNameConverter) {
        this.httpMethodsRepository = httpMethodsRepository;
        this.httpMethodsMapper = httpMethodsMapper;
        this.httpMethodsCache = httpMethodsCache;
        this.httpMethodNameConverter = httpMethodNameConverter;
    }

    @Override
    @Transactional
    public HttpMethodsResponseDto createHttpMethod(HttpMethodsRequestDto request) throws HttpMethodsOperationException {
        try {
            logger.info("Creating new HttpMethod: {}", request.methodName());
            
            HttpMethods httpMethod = httpMethodsMapper.toEntity(request);            
            httpMethod.setCreatedDttm(LocalDateTime.now());
            httpMethod.setLastUpdatedDttm(LocalDateTime.now());

            HttpMethods savedHttpMethod = httpMethodsRepository.save(httpMethod);
            logger.debug("HttpMethod created successfully: ID={}", savedHttpMethod.getId());

            return httpMethodsMapper.toDto(savedHttpMethod);
        } catch (Exception e) {
            logger.error("Failed to create httpMethod: ", e);
            throw new HttpMethodsOperationException("Failed to create httpMethod", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HttpMethodsResponseDto> findHttpMethodById(Integer id) throws HttpMethodsOperationException {
        try {
            logger.debug("Fetching httpMethod by ID: {}", id);
            return httpMethodsCache.get(id)
                .or(() -> {
                    try {
                    	HttpMethods httpMethod = httpMethodsRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("HealthCheck not found with ID: " + id));
                    	HttpMethodsResponseDto dto = httpMethodsMapper.toDto(httpMethod);
                    	httpMethodsCache.put(id, dto);
                        return Optional.of(dto);
                    } catch (ResourceNotFoundException e) {
                        logger.error("Error fetching httpMethod with ID: {}", id, e);
                        return Optional.empty(); // Return empty if not found
                    }
                });
        } catch (Exception e) {
            logger.error("Error fetching httpMethod with ID: {}", id, e);
            throw new HttpMethodsOperationException("Failed to fetch httpMethod", e);
        }
    }

    @Override
    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<List<HttpMethodsResponseDto>> findAllHttpMethodsAsync() {
        try {
            logger.info("Async fetching of all httpMethods initiated");
            List<HttpMethods> httpMethods = httpMethodsRepository.findAll();
            List<HttpMethodsResponseDto> response = httpMethods.stream()
                .peek(s -> logger.debug("Processing httpMethod: {}", s.getId()))
                .map(httpMethodsMapper::toDto)
                .collect(Collectors.toList());

            logger.info("Async httpMethod fetch completed successfully");
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            logger.error("Async httpMethod fetch failed", e);
            throw new CompletionException("Failed to fetch httpMethods asynchronously", e);
        }
    }

    @Override
    @Transactional
    public HttpMethodsResponseDto updateHttpMethod(Integer id, HttpMethodsRequestDto httpMethodsRequestDto) throws HttpMethodsOperationException {
        try {
            logger.info("Updating httpMethod ID: {}", id);
            HttpMethods httpMethod = httpMethodsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HttpMethod not found with ID: " + id));

            httpMethodsMapper.updateFromDto(httpMethodsRequestDto, httpMethod);
            httpMethod.setLastUpdatedDttm(LocalDateTime.now());
            
            HttpMethods updatedHttpMethod = httpMethodsRepository.save(httpMethod);
            httpMethodsCache.evict(id);
            logger.debug("HttpMethod updated successfully: ID={}", id);
            
            return httpMethodsMapper.toDto(updatedHttpMethod);
        } catch (Exception e) {
            logger.error("Failed to update HttpMethod ID: {}", id, e);
            throw new HttpMethodsOperationException("Failed to update httpMethod", e);
        }
    }

    @Override
    @Async
    @Transactional
    public ListenableFuture<Void> deleteHttpMethodAsync(Integer id) throws HttpMethodsOperationException {
        try {
            logger.info("Async deletion initiated for httpMethod ID: {}", id);
            httpMethodsRepository.deleteById(id);
            httpMethodsCache.evict(id);
            logger.debug("Async deletion completed for httpMethod ID: {}", id);
            return new AsyncResult<>(null);
        } catch (Exception e) {
            logger.error("Async deletion failed for httpMethod ID: {}", id, e);
            throw new HttpMethodsOperationException("Failed to delete httpMethod asynchronously", e);
        }
    }

	@Override
	public List<HttpMethodsResponseDto> findHttpMethodByMethodName(HttpMethodName httpMethodName) throws HttpMethodsOperationException {
		try {
            logger.debug("Fetching httpMethods with method name: {}", httpMethodName.name());
            
            HttpMethodName httpMethodNamee = HttpMethodName.valueOf(httpMethodName.name().toUpperCase());
            
            List<HttpMethodsResponseDto> dtos = httpMethodsRepository.findByMethodName(httpMethodNamee)
            		.stream()
            	    .map(httpMethodsMapper::toDto)
            	    .collect(Collectors.toList());
            
            
            
            return  dtos;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid httpMethodName status requested: {}", httpMethodName);
            throw new IllegalArgumentException("Invalid httpMethodName status: " + httpMethodName);
        } catch (Exception e) {
            logger.error("Error fetching httpMethodNames by status: {}", httpMethodName, e);
            throw new HttpMethodsOperationException("Failed to fetch httpMethodNames by status", e);
        }
	}
}
