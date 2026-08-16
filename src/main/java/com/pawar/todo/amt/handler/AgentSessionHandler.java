// package com.pawar.todo.amt.handler;


// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.messaging.simp.stomp.StompCommand;
// import org.springframework.messaging.simp.stomp.StompHeaders;
// import org.springframework.messaging.simp.stomp.StompSession;
// import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;


// import lombok.extern.slf4j.Slf4j;

// @Slf4j
// public class AgentSessionHandler extends StompSessionHandlerAdapter {
	
// 	private static final Logger logger = LoggerFactory.getLogger(AgentSessionHandler.class);

	
	
//     @Override
//     public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
//     	logger.info("New agent session established: {}", session.getSessionId());
        
//         // Send a test message upon connection
//         session.send("/app/command", "Test command from client");
//     }
//     @Override
//     public void handleException(StompSession session, StompCommand command, 
//                                StompHeaders headers, byte[] payload, Throwable exception) {
//     	logger.error("Agent WebSocket error: {}", exception.getMessage());
//     	exception.printStackTrace();
//     }
//     @Override
//     public void handleFrame(StompHeaders headers, Object payload) {
//     	logger.info("Received agent response: {}", payload);
//     }
// }
