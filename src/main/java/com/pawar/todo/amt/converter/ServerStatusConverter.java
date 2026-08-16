package com.pawar.todo.amt.converter;

import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.ServerStatus;

@Component
public class ServerStatusConverter {
	public ServerStatus toEnum(String status) {
		return ServerStatus.valueOf(status.toUpperCase());
	}

	public String toString(ServerStatus status) {
		return status.name();
	}
}