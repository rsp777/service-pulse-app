package com.pawar.todo.amt.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;


import com.pawar.todo.amt.constants.CommandRequest;
import com.pawar.todo.amt.constants.CommandResult;

@Service
public class CommandExecutionService {

    private final static Logger logger = LoggerFactory.getLogger(CommandExecutionService.class.getName());
	private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);//newCachedThreadPool(5);
    
    
    public void executeCommand(CommandRequest request, WebSocketSession session) {
    	String command = request.getCommand();
    	logger.info("request.getCommand() : {}",command);
        executorService.submit(() -> {
            try {
            	String os = System.getProperty("os.name").toLowerCase();
            	Process process;
            	 if (os.contains("win")) {
            		 logger.info("os : {}",os);
                     // Windows command execution
                     process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", command});
                 } else {
            		 logger.info("os : {}",os);
                     // Linux/Unix command execution
                     process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
                 }
            	
//                Process process = Runtime.getRuntime().exec(request.getCommand());
                logger.info("process : {}",process);
                activeProcesses.put(request.getRequestId(), process);
                
                BufferedReader outputReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
                );
                
                BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream())
                );
                
                // Stream output in real-time
                String line;
                StringBuilder output = new StringBuilder();
                while ((line = outputReader.readLine()) != null) {
                	output.append(line).append("\n");
                	session.sendMessage(new TextMessage(
                        new CommandResult(request.getRequestId(), "OUTPUT", line, null).toJson()
                    ));
                }
                
                // Check for errors
                StringBuilder errorOutput = new StringBuilder();
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
                
                int exitCode = process.waitFor();
                activeProcesses.remove(request.getRequestId());
                
                session.sendMessage(new TextMessage(
                    new CommandResult(
                        request.getRequestId(),
                        exitCode == 0 ? "COMPLETED" : "ERROR",
                        null,
                        exitCode != 0 ? errorOutput.toString() : null
                    ).toJson()
                ));
                logger.info("output : {}",output);
            } 
           catch (IOException e1) {
				logger.info("Error executing command : {}",e1.getMessage());
				try {
					session.sendMessage(new TextMessage(
					    new CommandResult(request.getRequestId(), "ERROR", null, e1.getMessage()).toJson()
					));
				} catch (Exception e2) {
					e1.printStackTrace();
				}
           	e1.printStackTrace();
			}
           catch (InterruptedException e) {
               try {
					session.sendMessage(new TextMessage(
					    new CommandResult(request.getRequestId(), "ERROR", null, e.getMessage()).toJson()
					));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
           }
            catch (Exception e) {
               try {
					session.sendMessage(new TextMessage(
					    new CommandResult(request.getRequestId(), "ERROR", null, e.getMessage()).toJson()
					));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
                // e.printStackTrace();
            }
            
        });
    }
    
    public boolean terminateCommand(String requestId) {
        Process process = activeProcesses.get(requestId);
        if (process != null) {
            process.destroy();
            activeProcesses.remove(requestId);
            return true;
        }
        return false;
    }
	
}
