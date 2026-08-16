package com.pawar.todo.amt.ssh;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.springframework.stereotype.Component;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

@Component
public class SSHConnection {

	private final Session session;

   public SSHConnection(Session session) {
       this.session = session;
   }

   public SSHResult execute(String command) throws Exception {
       ChannelExec channel = (ChannelExec) session.openChannel("exec");
       channel.setCommand(command);
       channel.setErrStream(System.err);

       BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
       channel.connect();

       StringBuilder output = new StringBuilder();
       String line;
       while ((line = reader.readLine()) != null) {
           output.append(line).append("\n");
       }

       channel.disconnect();
       return new SSHResult(output.toString(), channel.getExitStatus());
   }

   
   public void close() {
       if (session != null && session.isConnected()) {
           session.disconnect();
       }
   }
	
}
