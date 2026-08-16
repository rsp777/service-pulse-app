package com.pawar.todo.amt.respository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.app.healthcheck.dto.ServerResponseDto;
import com.pawar.app.healthcheck.dto.ServiceResponseDto;
import com.pawar.todo.amt.constants.ServerStatus;
import com.pawar.todo.amt.model.Server;
import com.pawar.todo.amt.model.Service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {

	Optional<ServiceResponseDto> findByServiceName(String serviceName);

	boolean existsByServiceName(String serviceName);


}
