package com.pawar.todo.amt.ssh;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class SshConnectionPool {

private static final Logger logger = LoggerFactory.getLogger(SshConnectionPool.class);

	
	
	 private final ConcurrentMap<String, SSHConnection> connectionPool = new ConcurrentHashMap<>();
	    private final JSch jsch = new JSch();
	    
	    public SSHConnection getConnection(String username, String password, String host, int port) throws Exception {
	        String connectionKey = host + ":" + port;
	        return connectionPool.computeIfAbsent(connectionKey, key -> createConnection(username, password, host, port));
	    }
	    
		public SSHConnection createConnection(String username, String password,String host,int port) {
	        try {
	            Session session = jsch.getSession(username, host, 22); // Replace "username" with actual username
                session.setPassword(password); // Replace with actual password or use key authentication
	            session.setConfig("StrictHostKeyChecking", "no");
	            session.connect();
                logger.info("SSH session created for host : {}",host);
	            return new SSHConnection(session);
	        } catch (Exception e) {
	            throw new RuntimeException("Failed to create SSH connection to " + host, e);
	        }
	    }
	
}
