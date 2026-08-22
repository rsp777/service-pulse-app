package com.pawar.todo.amt.service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import com.pawar.todo.amt.exceptions.AgentOperationException;
import com.pawar.todo.amt.exceptions.PathOperationException;
import com.pawar.todo.amt.exceptions.ResourceNotFoundException;
import com.pawar.todo.amt.exceptions.ServiceHealthStatusOperationException;
import com.pawar.todo.amt.exceptions.ServiceOperationException;

public interface ManageServices {

	public String startService(Integer serverId,Integer serviceId) throws AgentOperationException, ServiceOperationException, IOException, ServiceHealthStatusOperationException, PathOperationException, ResourceNotFoundException;
	public String stopService(Integer serverId,Integer serviceId) throws AgentOperationException, ServiceOperationException, IOException, ResourceNotFoundException, ServiceHealthStatusOperationException, PathOperationException;
	public String startAllServices(Integer serverId) throws AgentOperationException, IOException, PathOperationException, ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException;
	public String stopAllServices(Integer serverId) throws AgentOperationException, IOException, PathOperationException, ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException;
	public String restartAllServices(Integer serverId) throws AgentOperationException, IOException, PathOperationException, ResourceNotFoundException, ServiceHealthStatusOperationException, InterruptedException, ExecutionException;
	public void periodicServiceHealthCheck();
	

	
}
