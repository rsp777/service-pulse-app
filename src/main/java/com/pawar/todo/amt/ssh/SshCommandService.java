package com.pawar.todo.amt.ssh;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.pawar.app.healthcheck.dto.ServerResponseDto;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;

@Service
public class SshCommandService {

	private static final Logger logger = LoggerFactory.getLogger(SshCommandService.class);
	private final SshProperties properties;

	public SshCommandService(SshProperties properties) {
		this.properties = properties;
	}

	public String execute(ServerResponseDto server, String command) throws IOException {
		String host = resolveHost(server);
		logger.info("Starting SSH command on {} as {} (length={})", host, properties.getUsername(),
				command == null ? 0 : command.length());

		try (SSHClient client = connect(server);
				Session session = client.startSession();
				Command remoteCommand = session.exec(command)) {

			// 1. Wait for completion up to the specified timeout (returns void)
			remoteCommand.join(properties.getCommandTimeoutSeconds(), TimeUnit.SECONDS);

			// 2. Check if the channel is still open (indicates timeout)
			if (remoteCommand.isOpen()) {
				throw new IOException(String.format("Command timed out after %d seconds on %s",
						properties.getCommandTimeoutSeconds(), host));
			}

			// 3. Read output streams
			String output = new String(remoteCommand.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			String error = new String(remoteCommand.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
			Integer exitStatus = remoteCommand.getExitStatus();

			if (exitStatus == null || exitStatus != 0) {
				String errorDetails = StringUtils.hasText(error) ? error.trim() : "Exit code: " + exitStatus;
				logger.error("Remote command failed on {}: {}", host, errorDetails);
				if (errorDetails.contains("No such file or directory") || errorDetails.contains("not found")) {
					throw new IOException("SCRIPTS_UNAVAILABLE: Required script is unavailable on " + host + ": "
							+ errorDetails);
				}
				throw new IOException("Remote command failed: " + errorDetails);
			}

			logger.info("SSH command completed on {} with {} output characters", host, output.length());
			return output;
		}
		catch(Exception e) {
			logger.error("SSH command failed on {}: {}", host, e.getMessage(), e);
			throw new IOException("Error executing command: " + e.getMessage(), e);

		}
	}

	public boolean isReachable(ServerResponseDto server) {
		try (SSHClient client = connect(server)) {
			return client.isConnected() && client.isAuthenticated();
		} catch (Exception exception) {
			logger.debug("Server unreachable {}: {}", server.hostname(), exception.getMessage());
			return false;
		}
	}

	public void stream(ServerResponseDto server, String command, Consumer<String> lineConsumer,
			AtomicBoolean stopped) throws IOException {
		String host = resolveHost(server);
		logger.info("Starting SSH log stream on {} as {} (command length={})", host, properties.getUsername(),
				command == null ? 0 : command.length());

		try (SSHClient client = connect(server);
				Session session = client.startSession();
				Command remoteCommand = session.exec(command);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(remoteCommand.getInputStream(), StandardCharsets.UTF_8))) {

			String line;
			while (!stopped.get() && (remoteCommand.isOpen() || reader.ready())) {
				if (reader.ready()) {
					line = reader.readLine();
					if (line == null) {
						break;
					}
					lineConsumer.accept(line);
				} else {
					try {
						Thread.sleep(250L);
					} catch (InterruptedException exception) {
						Thread.currentThread().interrupt();
						stopped.set(true);
					}
				}
			}
			if (!stopped.get()) {
				String error = new String(remoteCommand.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
				Integer exitStatus = remoteCommand.getExitStatus();
				if (exitStatus == null || exitStatus != 0) {
					String errorDetails = StringUtils.hasText(error) ? error : "Exit code: " + exitStatus;
					if (errorDetails.contains("No such file or directory") || errorDetails.contains("not found")) {
						throw new IOException("SCRIPTS_UNAVAILABLE: Required script is unavailable on " + host + ": "
								+ errorDetails);
					}
					throw new IOException("Remote command failed: " + errorDetails);
				}
			}
			remoteCommand.close();
			logger.info("SSH log stream completed for {}", host);
		} catch (IOException exception) {
			if (!stopped.get()) {
				logger.error("SSH log stream failed for {}: {}", host, exception.getMessage(), exception);
				throw exception;
			}
			logger.debug("Log stream closed for {}", host);
		}
	}

	private SSHClient connect(ServerResponseDto server) throws IOException {
		validateConfiguration();
		SSHClient client = new SSHClient();

		try {
			// Configure Host Key Verification
			configureHostKeyVerifier(client);

			client.setConnectTimeout(properties.getConnectTimeoutMillis());

			String host = resolveHost(server);
			int port = (properties.getPort() != 0 && properties.getPort() > 0) ? properties.getPort() : 22;

			client.connect(host, port);

			// Load Private Key
			KeyProvider keyProvider;
			if (StringUtils.hasText(properties.getPrivateKeyPassphrase())) {
				keyProvider = client.loadKeys(properties.getPrivateKeyPath(), properties.getPrivateKeyPassphrase());
			} else {
				keyProvider = client.loadKeys(properties.getPrivateKeyPath());
			}

			client.authPublickey(properties.getUsername(), keyProvider);
			return client;

		} catch (IOException exception) {
			client.close();
			throw exception;
		}
	}

	private void configureHostKeyVerifier(SSHClient client) throws IOException {
		// If knownHostsPath is specified and strict checking is enabled, load
		// known_hosts
		if (properties.isStrictHostKeyChecking() && StringUtils.hasText(properties.getKnownHostsPath())) {
			File knownHosts = new File(properties.getKnownHostsPath());
			if (knownHosts.exists()) {
				client.loadKnownHosts(knownHosts);
				return;
			}
			logger.warn("known_hosts file not found at {}. Falling back to PromiscuousVerifier.",
					properties.getKnownHostsPath());
		}

		// Trust all host keys (resolves the ed25519 TransportException)
		client.addHostKeyVerifier(new PromiscuousVerifier());
	}

	private String resolveHost(ServerResponseDto server) throws IOException {
		if (server == null) {
			throw new IOException("Server details cannot be null");
		}
		String host = StringUtils.hasText(server.ipAddress()) ? server.ipAddress() : server.hostname();
		if (!StringUtils.hasText(host)) {
			throw new IOException("Server must have an IP address or hostname");
		}
		return host;
	}

	private void validateConfiguration() throws IOException {
		if (!StringUtils.hasText(properties.getUsername()) || !StringUtils.hasText(properties.getPrivateKeyPath())) {
			throw new IOException("SSH_USERNAME and SSH_PRIVATE_KEY_PATH must be configured");
		}
	}
}