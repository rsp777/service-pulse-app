package com.pawar.todo.amt.service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;

public interface LogsService {
	public String viewLogsByService(Integer serverId, Integer serviceId)
			throws AgentOperationException, IOException, ServiceOperationException, CommandOperationException, PathOperationException;

	void streamLogsByService(Integer serverId, Integer serviceId, Consumer<String> lineConsumer,
			AtomicBoolean stopped)
			throws AgentOperationException, IOException, ServiceOperationException, CommandOperationException,
			PathOperationException;
}