package com.pawar.todo.amt.handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pawar.todo.amt.constants.CommandResult;
import com.pawar.todo.amt.controller.LogsController;
import com.pawar.todo.amt.controller.ManageServicesController;
import com.pawar.todo.amt.controller.WebSocketAgentController;
import com.pawar.todo.amt.service.WebSocketAgentService;

@Component
public class CommandWebSocketHandler extends TextWebSocketHandler {

	private static final Logger logger = LoggerFactory.getLogger(CommandWebSocketHandler.class);

	private CompletableFuture<String> responseFuture = new CompletableFuture<>();
	private WebSocketAgentController agentController;
	private ManageServicesController manageServicesController;
	private LogsController logsController;
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private final ExecutorService executorService = Executors.newFixedThreadPool(1000);

	public CommandWebSocketHandler() {

	}

	@Autowired
	public void setWebSocketAgentService(WebSocketAgentService webSocketAgentService) {
	}

	@Autowired
	public void setWebSocketAgentController(WebSocketAgentController agentController) {
		this.agentController = agentController; // Setter injection
	}

	@Autowired
	public void setManageServicesController(ManageServicesController manageServicesController) {
		this.manageServicesController = manageServicesController; // Setter injection
	}

	@Autowired
	public void setLogsController(LogsController logsController) {
		this.logsController = logsController; // Setter injection
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		try {
			logger.info("Client is connect to the websocket server : " + session.getRemoteAddress());
			session.setTextMessageSizeLimit(2000000);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		logger.info("WebSocket connection closed: {} with status: {}", session.getId(), closeStatus);

	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
		exception.printStackTrace();

	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message)
			throws JsonMappingException, JsonProcessingException {
		executorService.submit(() -> {
			try {
				String base64Message = message.getPayload();
				byte[] compressedData = Base64.getDecoder().decode(base64Message);
				logger.debug("compressedData: {}", compressedData);

				byte[] decompressedData = decompressData(compressedData);

				String decompressedMessage = new String(decompressedData, StandardCharsets.UTF_8);
				
				if (!decompressedMessage.startsWith("{")) {
					decompressedMessage = "{" + decompressedMessage;
				}
				logger.debug("Decompressed message: {}", decompressedMessage);
				logger.info("Decompressed length: {}", decompressedMessage.length());

				if (decompressedMessage.contains("requestId") && decompressedMessage.contains("status") && decompressedMessage.contains("output")
						&& decompressedMessage.contains("error")) {
					CommandResult result = new CommandResult();
					// logger.info("decompressedMessage : {}",decompressedMessage);		
					// objectMapper.getFactory()
					// .setStreamReadConstraints(StreamReadConstraints.builder()
					// .maxStringLength(20_000_000).build());
					result = objectMapper.readValue(decompressedMessage, CommandResult.class);

					if (result.getStatus().equals("COMPLETED")) {
						agentController.onMessageReceived(new TextMessage(decompressedMessage));
						manageServicesController.onMessageReceived(new TextMessage(decompressedMessage));
						logsController.onMessageReceived(new TextMessage(decompressedMessage));
					} else if (result.getStatus().equals("ERROR")) {
						agentController.onMessageReceived(new TextMessage(decompressedMessage));
						manageServicesController.onMessageReceived(new TextMessage(decompressedMessage));
						logsController.onMessageReceived(new TextMessage(decompressedMessage));

					}
				}
				logger.debug("message : {}", message.getPayload());
			} catch (Exception e) {
				logger.error("Error Sending Message to Controller", session, message);
			}
		});
	}

	public void onMessageReceived(String message) {
		logger.info("onMessageReceived: {}", message);
		responseFuture.complete(message);
	}

	public byte[] decompressData(byte[] compressedData) throws IOException {
		if (compressedData == null || compressedData.length == 0) {
			throw new IllegalArgumentException("Input data cannot be null or empty");
		}

		// Log the first few bytes to check if it looks like GZIP data
		logger.info("Compressed data length: {}", compressedData.length);
		logger.info("First few bytes: {}",
				Arrays.toString(Arrays.copyOf(compressedData, Math.min(10, compressedData.length))));

		try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
				GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
				ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

			byte[] buffer = new byte[1024];
			int length;
			logger.info("gzipInputStream : {}", gzipInputStream.read());

			while ((length = gzipInputStream.read(buffer)) != -1) {
				byteArrayOutputStream.write(buffer, 0, length);				
			}

			logger.info("decompess data : {}", byteArrayOutputStream.toString().length());
			return byteArrayOutputStream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException("Error occurred during data decompression", e);
		}
	}

}
