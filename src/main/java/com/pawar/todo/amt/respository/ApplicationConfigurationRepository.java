package com.pawar.todo.amt.respository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pawar.todo.amt.model.ApplicationConfiguration;

public interface ApplicationConfigurationRepository extends JpaRepository<ApplicationConfiguration, String> {
}
