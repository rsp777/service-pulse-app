package com.pawar.todo.amt.respository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.app.healthcheck.dto.PathResponseDto;
import com.pawar.todo.amt.model.Path;

import jakarta.validation.constraints.NotBlank;

@Repository
public interface PathRepository extends JpaRepository<Path, Integer> {

	boolean existsByPathName(@NotBlank String pathName);

	boolean existsByPathDescription(String pathDescription);

	Optional<Path> findByPathDescription(String pathDescription);

	Optional<Path> findByPathName(String pathName);
}