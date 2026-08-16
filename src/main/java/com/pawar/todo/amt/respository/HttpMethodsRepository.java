package com.pawar.todo.amt.respository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.app.healthcheck.dto.HttpMethodsResponseDto;
import com.pawar.todo.amt.constants.HttpMethodName;
import com.pawar.todo.amt.model.Command;
import com.pawar.todo.amt.model.HealthCheck;
import com.pawar.todo.amt.model.HttpMethods;
import com.pawar.todo.amt.model.Path;

import jakarta.validation.constraints.NotBlank;

@Repository
public interface HttpMethodsRepository extends JpaRepository<HttpMethods, Integer> {

	Optional<HttpMethods> findByMethodName(HttpMethodName httpMethodNamee);

//	Optional<HttpMethods> findByStatus(HttpMethodName httpMethodNamee);

}