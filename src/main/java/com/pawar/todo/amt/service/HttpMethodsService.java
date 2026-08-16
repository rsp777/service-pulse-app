package com.pawar.todo.amt.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.CommandRequestDto;
import com.pawar.app.healthcheck.dto.CommandResponseDto;
import com.pawar.app.healthcheck.dto.HttpMethodsRequestDto;
import com.pawar.app.healthcheck.dto.HttpMethodsResponseDto;
import com.pawar.app.healthcheck.dto.ServerRequestDto;
import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.HttpMethodName;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.HttpMethodsOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServerOperationException;
import com.pawar.todo.amt.model.Server;

public interface HttpMethodsService {
	 public HttpMethodsResponseDto createHttpMethod(HttpMethodsRequestDto httpMethodsRequestDto )throws HttpMethodsOperationException;
	 public Optional<HttpMethodsResponseDto> findHttpMethodById(Integer id) throws HttpMethodsOperationException;
	 public CompletableFuture<List<HttpMethodsResponseDto>> findAllHttpMethodsAsync();
	 public HttpMethodsResponseDto updateHttpMethod(Integer id, HttpMethodsRequestDto httpMethodsRequestDto) throws HttpMethodsOperationException;
	 public ListenableFuture<Void> deleteHttpMethodAsync(Integer id) throws HttpMethodsOperationException;
	 public List<HttpMethodsResponseDto> findHttpMethodByMethodName(HttpMethodName httpMethodName) throws HttpMethodsOperationException;
}