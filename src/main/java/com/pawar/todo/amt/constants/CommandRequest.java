package com.pawar.todo.amt.constants;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandRequest {
	private String requestId;
    private String command;
    private String username;
    private String password; 
    private String host;
    private int port;
    private Map<String, String> metadata;
}
