package com.pawar.todo.amt.respository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pawar.todo.amt.constants.AgentStatus;
import com.pawar.todo.amt.model.Agent;
import com.pawar.todo.amt.model.Server;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Integer> {

	Optional<Agent> findByStatus(AgentStatus agentStatus);

	Optional<Agent> findAgentByServerId(Integer id);

}