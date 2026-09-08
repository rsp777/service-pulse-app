package com.pawar.todo.amt.respository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.todo.amt.model.ServerServiceConfiguration;

@Repository
public interface ServerServiceConfigurationRepository extends JpaRepository<ServerServiceConfiguration, Integer> {

    List<ServerServiceConfiguration> findByServerId(Integer serverId);

    Optional<ServerServiceConfiguration> findByServerIdAndServiceId(Integer serverId, Integer serviceId);

    void deleteByServiceId(Integer serviceId);
}
