package com.pawar.todo.amt.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.util.concurrent.ListenableFuture;

import com.pawar.app.healthcheck.dto.ServiceHealthStatusRequestDto;
import com.pawar.app.healthcheck.dto.ServiceHealthStatusResponseDto;
import com.pawar.todo.amt.constants.HealthCheckStatus;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceHealthStatusOperationException;

public interface ServiceHealthStatusService {
	public ServiceHealthStatusResponseDto createServiceHealthStatus(
			ServiceHealthStatusRequestDto serviceHealthStatusRequestDto) throws ServiceHealthStatusOperationException;

	public Optional<ServiceHealthStatusResponseDto> findServiceHealthStatusById(Integer id)
			throws ServiceHealthStatusOperationException;

	public CompletableFuture<List<ServiceHealthStatusResponseDto>> findAllServiceHealthStatussAsync()
			throws ServiceHealthStatusOperationException;

	public ServiceHealthStatusResponseDto updateServiceHealthStatus(Integer id,
			ServiceHealthStatusRequestDto healthCheckRequestDto) throws ServiceHealthStatusOperationException;

	public ListenableFuture<Void> deleteServiceHealthStatusAsync(Integer id)
			throws ServiceHealthStatusOperationException;

	public List<ServiceHealthStatusResponseDto> findServiceHealthStatusByStatus(HealthCheckStatus status)
			throws ServiceHealthStatusOperationException;

	public Optional<List<ServiceHealthStatusResponseDto>> findServiceHealthStatusByServerId(Integer id) throws ServiceHealthStatusOperationException, ResourceNotFoundException;

	public Optional<ServiceHealthStatusResponseDto> findServiceHealthStatusByServiceId(Integer serviceId) throws ServiceHealthStatusOperationException;

	public ServiceHealthStatusResponseDto updateServiceHealthStatus(Integer id, ServiceHealthStatusResponseDto healthStatusResponseDto) throws ServiceHealthStatusOperationException;
}