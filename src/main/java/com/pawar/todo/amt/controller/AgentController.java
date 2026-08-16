package com.pawar.todo.amt.controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pawar.app.healthcheck.dto.AgentRequestDto;
import com.pawar.app.healthcheck.dto.AgentResponseDto;
import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.service.AgentService;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT_LOGGER");

    private final AgentService agentService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5); // Thread pool for async operations

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping
    public ResponseEntity<AgentResponseDto> createAgent(@RequestBody AgentRequestDto request) {
        logger.info("Received request to create new agent: {}", request.name());
        logger.debug("Agent creation request details: {}", request);

        try {
            CompletableFuture<AgentResponseDto> createdAgentFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return agentService.createAgent(request);
                } catch (AgentOperationException e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

            AgentResponseDto createdAgent = createdAgentFuture.join(); // Wait for the result
            auditLogger.info("Agent created successfully - ID: {}, Name: {}", createdAgent.id(), createdAgent.name());
            logger.debug("Created agent details: {}", createdAgent);

            return ResponseEntity.status(HttpStatus.CREATED).body(createdAgent);
        } catch (Exception e) {
            logger.error("Failed to create agent: {} - Error: {}", request.name(), e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentResponseDto> getAgentById(@PathVariable Integer id) {
        logger.info("Fetching agent by ID: {}", id);

        try {
            Optional<AgentResponseDto> agent = agentService.findAgentById(id);

            if (agent.isPresent()) {
                logger.info("Fetched agent : {}", agent);
                logger.debug("Retrieved agent details for ID {}: {}", id, agent.get());
                return ResponseEntity.ok(agent.get());
            } else {
                logger.warn("Agent not found with ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (AgentOperationException e) {
            logger.error("Error fetching agent ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/server/{id}")
    public ResponseEntity<AgentResponseDto> getAgentByServerId(@PathVariable Integer id) {
        logger.info("Fetching agent by server ID: {}", id);

        try {
            Optional<AgentResponseDto> agent = agentService.findAgentByServerId(id);

            if (agent.isPresent()) {
                logger.info("Fetched agent : {}", agent);
                logger.debug("Retrieved agent details for server ID {}: {}", id, agent.get());
                return ResponseEntity.ok(agent.get());
            } else {
                logger.warn("Agent not found with server ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (AgentOperationException e) {
            logger.error("Error fetching agent server ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<AgentResponseDto>> getAllAgents() {
        logger.info("Fetching all agents");

        try {
            CompletableFuture<List<AgentResponseDto>> agentsFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return agentService.findAllAgentsAsync().join(); // Wait for the async operation
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

            List<AgentResponseDto> agents = agentsFuture.join(); // Wait for the result
            logger.debug("Retrieved {} agents", agents.size());
            return ResponseEntity.ok(agents);
        } catch (Exception e) {
            logger.error("Error fetching all agents: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgentResponseDto> updateAgent(@PathVariable Integer id,
            @RequestBody AgentRequestDto request) {
        logger.info("Updating agent ID: {}", id);
        logger.debug("Update details for agent ID {}: {}", id, request);

        try {
            CompletableFuture<AgentResponseDto> updatedAgentFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return agentService.updateAgent(id, request);
                } catch (AgentOperationException e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

            AgentResponseDto updatedAgent = updatedAgentFuture.join(); // Wait for the result
            auditLogger.info("Agent updated - ID: {}, Name: {}", id, updatedAgent.name());
            logger.debug("Updated agent details: {}", updatedAgent);

            return ResponseEntity.ok(updatedAgent);
        } catch (Exception e) {
            logger.error("Failed to update agent ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(@PathVariable Integer id) {
        logger.info("Deleting agent ID: {}", id);

        try {
            CompletableFuture<Void> deleteFuture = CompletableFuture.runAsync(() -> {
                try {
                    agentService.deleteAgentAsync(id);
                } catch (AgentOperationException e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

            deleteFuture.join(); // Wait for the deletion to complete
            auditLogger.warn("Agent deleted - ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Failed to delete agent ID {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
