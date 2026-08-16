package com.pawar.todo.amt.constants;

public enum ScriptExtension {
	SH(".sh"), // Shell script
    PY(".py"), // Python script
    JS(".js"), // JavaScript
    BAT(".bat"), // Batch file
    PS1(".ps1"); // PowerShell script
    
	private final String extension;
    ScriptExtension(String extension) {
        this.extension = extension;
    }
    public String getExtension() {
        return extension;
    }
}
