package com.pawar.todo.amt.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.pawar.todo.amt.client.WebSocketAgentClient;
import com.pawar.todo.amt.exceptions.AgentOperationException;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WebSocketAgentService {

	private static final Logger logger = LoggerFactory.getLogger(WebSocketAgentService.class);
	private final SimpMessagingTemplate messagingTemplate;
	private WebSocketAgentClient webSocketAgentClient;

	@Autowired
	public WebSocketAgentService(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;

	}

	@Autowired
	public void setWebSocketAgentClient(WebSocketAgentClient webSocketAgentClient) {
		this.webSocketAgentClient = webSocketAgentClient; // Setter injection
	}

	public String sendCommand(Integer agentId, String command) throws IOException, AgentOperationException {
		logger.info("Sending command to agent: {}", command);
		String response = webSocketAgentClient.sendMessage(agentId, command);
		logger.info("Command sent to agent: {}", command);
		return response;
	}

	public void sendMessageToAgent(String message) {
		logger.info("Sending message to agent: {}", message);
		messagingTemplate.convertAndSend("/topic/messages", message);
	}

	public void switchAgent(Integer id) throws AgentOperationException {
		webSocketAgentClient.switchAgent(id);
	}
	
}