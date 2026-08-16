package com.pawar.todo.amt.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.CommandStatus;
import com.pawar.todo.amt.constants.HttpMethodName;
import com.pawar.todo.amt.constants.ScriptExtension;
import com.pawar.todo.amt.constants.ServerStatus;

@Component
public class CommandStatusConverter {
	
	
	private final static Logger logger = LoggerFactory.getLogger(CommandStatusConverter.class.getName());

	public CommandStatus toEnum(String status) {
		
		logger.info("status : {}",status);
		
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        try {
        	
            return CommandStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status, e);
        }
    }
    public String toString(CommandStatus status) {
		logger.info("status : {}",status);

    	if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        return status.name();
    }
    // Optional: Method to get enum from extension string
    public static CommandStatus fromExtension(String status) {
		logger.info("status : {}",status);

    	for (CommandStatus commandStatus : CommandStatus.values()) {
            if (commandStatus.name().equalsIgnoreCase(status)) {
                return commandStatus;
            }
        }
        throw new IllegalArgumentException("No enum constant for status: " + status);
    }
}
