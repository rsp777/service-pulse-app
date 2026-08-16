package com.pawar.todo.amt.service;

import java.io.IOException;

import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.exceptions.CommandOperationException;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;

public interface LogsService {
	public String viewLogsByService(Integer agentId, Integer serviceId)
			throws AgentOperationException, IOException, ServiceOperationException, CommandOperationException, PathOperationException;
}