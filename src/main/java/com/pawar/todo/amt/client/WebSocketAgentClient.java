package com.pawar.todo.amt.client;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.util.UriTemplate;

import com.pawar.app.healthcheck.dto.AgentResponseDto;
import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.handler.CommandWebSocketHandler;
import com.pawar.todo.amt.service.AgentService;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@Component
public class WebSocketAgentClient {

    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY = 2000;
    private static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private WebSocketSession session;

    private AgentService agentService;

    private CommandWebSocketHandler commandWebSocketHandler;
    private final RestTemplate restTemplate = new RestTemplate(); // Reused instance
    private final WebSocketClient webSocketClient = new StandardWebSocketClient(); // Reused instance

    static String agentWebSocketUrl = "";
    static String healthCheckUrl = "";

    @Autowired
    public void setAgentService(AgentService agentService) {
        this.agentService = agentService; // Setter injection

    }
    
    @Autowired
    public void setCommandWebSocketHandler(CommandWebSocketHandler commandWebSocketHandler) {
        this.commandWebSocketHandler = commandWebSocketHandler; // Setter injection
    }

    public String sendMessage(Integer id, String message) throws IOException, AgentOperationException {
        String encodedCommand = URLEncoder.encode(message, "UTF-8");
        Optional<AgentResponseDto> agentResponseDto = getAgent(id);
        if (agentResponseDto.isEmpty()) {
            return "No agent found with id: " + id;
        }

        if (session == null || !session.isOpen()) {
            connectToAgent(id);
        }

        if (session != null && !agentWebSocketUrl.equals(getAgentWebSocketURL(id))) {
            log.info("Switching Agent");
            switchAgent(id); // Switch to the new agent
        }

        if (session != null && session.isOpen()) {
            log.info("Connected to agent WebSocket at: {}", agentWebSocketUrl);
            TextMessage textMessage = new TextMessage(encodedCommand);
            session.sendMessage(textMessage);
            log.info("Command sent to: {}", session.getRemoteAddress());
            return "Message sent to the server";
        } else {
            log.warn("WebSocket session is closed. Cannot send message.");
            return "WebSocket session is closed. Cannot send message.";
        }
    }

    public boolean connectToAgent(Integer id) throws AgentOperationException {
        agentWebSocketUrl = getAgentWebSocketURL(id);
        healthCheckUrl = getAgentHealthCheckURL(id);
        if (isServerHealthy(healthCheckUrl)) {
            for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                try {
                    UriTemplate uriTemplate = new UriTemplate(agentWebSocketUrl);
                    ListenableFuture<WebSocketSession> listenableFuture = webSocketClient
                            .doHandshake(commandWebSocketHandler, uriTemplate.toString(), uriTemplate.toString());
                    session = listenableFuture.get(); // Wait for the connection to complete
                    sessions.add(session);
                    log.info("Connected to Agent: sessionId = {} and Address = {}", session.getId(),
                            session.getRemoteAddress());
                    return true;
                } catch (Exception e) {
                    log.error("Connection attempt {} failed: {}", attempt + 1, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            log.error("Failed to connect to agent WebSocket after {} attempts.", MAX_RETRIES);
            return false;
        } else {
            log.error("Server is not healthy. Aborting connection attempt.");
            return false;
        }
    }

    private boolean isServerHealthy(String healthCheckUrl) {
        try {
            log.info("healthCheckUrl: {}", healthCheckUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(healthCheckUrl, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return false;
        }
    }

    private String getAgentWebSocketURL(Integer id) throws AgentOperationException {
        return getAgent(id).map(AgentResponseDto::agentWebSocketUrl).orElseThrow(() -> new AgentOperationException("Agent not found"));
    }

    private String getAgentHealthCheckURL(Integer id) throws AgentOperationException {
        AgentResponseDto agentResponseDto = getAgent(id).orElseThrow(() -> new AgentOperationException("Agent not found"));
        return "http://" + agentResponseDto.host() + ":" + agentResponseDto.port() + "/service-pulse-agent/actuator/health";
    }

    public void switchAgent(Integer id) throws AgentOperationException {
        if (session != null && session.isOpen()) {
            try {
                session.close(); // Close the current session
            } catch (IOException e) {
                log.error("Error closing WebSocket session: {}", e.getMessage());
            }
        }
        connectToAgent(id);
    }

    public Optional<AgentResponseDto> getAgent(Integer id) throws AgentOperationException {
        return agentService.findAgentById(id);
    }

    public boolean checkConnectionStatus(Integer agentId) throws AgentOperationException {
        boolean connectionStatus = connectToAgent(agentId);
        log.info("Client: {} connection status to server: {}", agentId, connectionStatus);
        return connectionStatus;
    }
}