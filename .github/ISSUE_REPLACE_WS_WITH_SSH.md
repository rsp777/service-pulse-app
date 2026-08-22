Title: Replace WebSocket-based remote command execution with SSH key-based execution

## Summary

Replace the current WebSocket-based remote command execution (agent WebSocket server / `WebSocketAgentClient`) with SSH key-based command execution to remote Linux servers. Use SSH key authentication (and host-key verification) rather than running commands over a WebSocket server on target machines.

## Why

- WebSocket-based remote execution requires a custom websocket server running on every target machine and increases attack surface.
- Key-based SSH is standard, more secure, and easier to integrate with existing infrastructure and automation (deploy keys, SSH agents, configuration management).
- The project contains an unused `ssh` package that currently uses password auth via JSch; this should be replaced/updated to a key-based, secure client.

## Current state (examples)
- WebSocket client/service: `src/main/java/com/pawar/todo/amt/client/WebSocketAgentClient.java`
- WebSocket handler: `src/main/java/com/pawar/todo/amt/handler/CommandWebSocketHandler.java`
- WebSocket service wrapper: `src/main/java/com/pawar/todo/amt/service/WebSocketAgentService.java`
- Unused SSH helpers: `src/main/java/com/pawar/todo/amt/ssh/` (contains `SshConnectionPool`, `SSHConnection`, `SSHResult`)

## Proposal
1. Replace or augment `WebSocketAgentClient`/`WebSocketAgentService` with a secure SSH client implementation that:
   - Uses SSH key authentication (private key files, optional passphrase, or SSH agent forwarding).
   - Verifies host keys (do not use `StrictHostKeyChecking=no` in production).
   - Provides connection pooling and proper session lifecycle (reconnect/backoff).
   - Logs minimally and avoids leaking secrets.
2. Prefer a modern SSH client library (recommend `sshj` or Apache MINA SSHD client) instead of JSch, or update the existing `SshConnectionPool` to addIdentity() + known_hosts handling if we keep JSch.
3. Add configuration options in `application.properties` for key paths, passphrase store mechanism, host-key file, connection timeout, and connection pool size.
4. Update service layer to call the SSH client instead of sending WebSocket messages for remote command execution. Keep WebSocket path only if agents still require it for other interactions.
5. Add migration steps, tests, and an integration test that runs commands (in a CI job that uses a test container VM or GitHub Actions runner with known SSH test server).

## Security considerations
- Store private keys securely (do not commit to repo). Use environment variables, secrets manager, or mount as files in deployment.
- Enable host-key verification and provide a way to bootstrap known_hosts safely.
- Use least-privilege accounts on remote hosts.

## Files likely affected
- `src/main/java/com/pawar/todo/amt/client/WebSocketAgentClient.java`
- `src/main/java/com/pawar/todo/amt/service/WebSocketAgentService.java`
- `src/main/java/com/pawar/todo/amt/handler/CommandWebSocketHandler.java`
- `src/main/java/com/pawar/todo/amt/service/ManageServicesImpl.java` (calls that currently go through WebSocket service)
- `src/main/java/com/pawar/todo/amt/ssh/*` (refactor or replace)
- `src/main/resources/application.properties`

## Migration steps (high level)
1. Decide whether to remove WebSocket-based execution or keep both (feature-flagged).
2. Implement SSH client with key auth and host-key verification.
3. Add configuration and secrets handling.
4. Update service layer to call SSH client.
5. Add tests and update CI to run integration test.
6. Document in README + deployment notes.

## Acceptance criteria
- Remote commands run over SSH with key-based auth.
- Host-key verification enabled by default.
- No private keys are stored in source.
- Tests and docs updated.


Please assign to the team/owner and label as `enhancement` / `security` as appropriate.
