package com.pawar.todo.amt.respository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.app.healthcheck.dto.ScriptResponseDto;
import com.pawar.todo.amt.model.Script;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Repository
public interface ScriptRepository extends JpaRepository<Script, Integer> {

	boolean existsByScriptName(@NotBlank String scriptName);

	boolean existsByScriptName(Script entity);

	Optional<Script> findByScriptName(String scriptName);


	
}
