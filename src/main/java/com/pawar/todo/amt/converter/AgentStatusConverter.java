package com.pawar.todo.amt.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.pawar.todo.amt.constants.AgentStatus;

@Component
public class AgentStatusConverter {

    private final static Logger logger = LoggerFactory.getLogger(AgentStatusConverter.class.getName());

    public AgentStatus toEnum(String status) {

        logger.info("status : {}", status);

        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        try {

            return AgentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status, e);
        }
    }

    public String toString(AgentStatus status) {
        logger.info("status : {}", status);

        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }
        return status.name();
    }

    public static AgentStatus fromExtension(String status) {
        logger.info("status : {}", status);

        for (AgentStatus agentStatus : AgentStatus.values()) {
            if (agentStatus.name().equalsIgnoreCase(status)) {
                return agentStatus;
            }
        }
        throw new IllegalArgumentException("No enum constant for status: " + status);
    }
}