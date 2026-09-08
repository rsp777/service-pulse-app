package com.pawar.todo.amt.respository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pawar.todo.amt.model.AlertConfiguration;

public interface AlertConfigurationRepository extends JpaRepository<AlertConfiguration, Integer> {
    List<AlertConfiguration> findByEnabledTrue();
}
