package com.pawar.todo.amt.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pawar.todo.amt.constants.ScriptExtension;

@Component
public class ScriptExtensionConverter {

    private final static Logger logger = LoggerFactory.getLogger(ScriptExtensionConverter.class.getName());

    public ScriptExtension toEnum(String scriptExtension) {

        logger.info("scriptExtension : {}", scriptExtension);

        if (scriptExtension == null) {
            throw new IllegalArgumentException("Script extension cannot be null");
        }
        try {

            return ScriptExtension.valueOf(scriptExtension.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid script extension: " + scriptExtension, e);
        }
    }

    public String toString(ScriptExtension scriptExtension) {
        logger.info("scriptExtension : {}", scriptExtension);

        if (scriptExtension == null) {
            throw new IllegalArgumentException("Script extension cannot be null");
        }
        return scriptExtension.getExtension();
    }

    // Optional: Method to get enum from extension string
    public static ScriptExtension fromExtension(String extension) {
        logger.info("scriptExtension : {}", extension);

        for (ScriptExtension scriptExtension : ScriptExtension.values()) {
            if (scriptExtension.getExtension().equalsIgnoreCase(extension)) {
                return scriptExtension;
            }
        }
        throw new IllegalArgumentException("No enum constant for extension: " + extension);
    }
}