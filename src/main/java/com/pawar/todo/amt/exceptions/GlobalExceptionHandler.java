package com.pawar.todo.amt.exceptions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.pawar.app.healthcheck.dto.ServerErrorResponseDto;
import com.pawar.app.healthcheck.dto.ServiceErrorResponseDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ServerErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex,
			HttpServletRequest request) {
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.NOT_FOUND.value(), "Not Found",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
	}
	
	@ExceptionHandler(ServiceOperationException.class)
	public ResponseEntity<ServiceErrorResponseDto> handleServiceOperationException(ServiceOperationException ex,
			HttpServletRequest request) {
		String path = request.getRequestURI();
		ServiceErrorResponseDto errorResponse = new ServiceErrorResponseDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}
	
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ServerErrorResponseDto> handleResourceNotFoundException(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	 @ExceptionHandler(ValidationException.class)
	    public ResponseEntity<ServerErrorResponseDto> handleValidationException(ValidationException ex, HttpServletRequest request) {
	        Map<String, String> validationErrors = new HashMap();
	        // Populate validationErrors based on the exception details
	        validationErrors.put("fieldName", "error message");

	        String path = request.getRequestURI(); // Get the actual request URI
	        ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(
	            HttpStatus.BAD_REQUEST.value(),
	            "Validation Error",
	            "There were validation errors",
	            path // Use the dynamic path here
	        ).withValidationErrors(validationErrors);

	        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	    }

	@ExceptionHandler(ResourceAlreadyExistsException.class)
	public ResponseEntity<ServerErrorResponseDto> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex,HttpServletRequest request) {
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.CONFLICT.value(), "CONFLICT",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(OperationNotAllowedException.class)
	public ResponseEntity<ServerErrorResponseDto> handleOperationNotAllowedException(OperationNotAllowedException ex,HttpServletRequest request) {
		
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(ExternalServiceException.class)
	public ResponseEntity<ServerErrorResponseDto> handleExternalServiceException(ExternalServiceException ex,HttpServletRequest request) {
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.SERVICE_UNAVAILABLE.value(), "SERVICE_UNAVAILABLE",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		
		return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ServerErrorResponseDto> handleDataIntegrityViolationException(DataIntegrityViolationException ex,HttpServletRequest request) {
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.CONFLICT.value(), "CONFLICT",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		
		return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
	}

	@ExceptionHandler(InvalidStateException.class)
	public ResponseEntity<ServerErrorResponseDto> handleInvalidStateException(InvalidStateException ex,HttpServletRequest request) {
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		
		return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(TimeoutException.class)
	public ResponseEntity<ServerErrorResponseDto> handleTimeoutException(TimeoutException ex,HttpServletRequest request) {
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.REQUEST_TIMEOUT.value(), "REQUEST_TIMEOUT",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		
		return new ResponseEntity<>(errorResponse, HttpStatus.REQUEST_TIMEOUT);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ServerErrorResponseDto> handleCustomBusinessException(BusinessException ex,HttpServletRequest request) {
		
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ServerErrorResponseDto> handleGlobalException(Exception ex,HttpServletRequest request) {
		String path = request.getRequestURI();
		ServerErrorResponseDto errorResponse = new ServerErrorResponseDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR",
				ex.getMessage(), path // You can dynamically set this based on the request
		);
		return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
