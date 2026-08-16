package com.pawar.todo.amt.ssh;

public class SSHResult {
	private final String output;
   private final int exitCode;

   public SSHResult(String output, int exitCode) {
       this.output = output;
       this.exitCode = exitCode;
   }

   public String getOutput() {
       return output;
   }

   public int getExitCode() {
       return exitCode;
   }
}
