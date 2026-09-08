# Service Pulse App

## Project Description
The Service Pulse App is a Spring Boot application designed to monitor the health and status of various services and manage remote agents that perform checks and execute commands. It provides a central system for tracking service availability, performance, and for orchestrating tasks on remote servers.

## Features

*   **Service Health Monitoring:** Tracks the health status (`UP`, `DOWN`, `UNKNOWN`), response time, and error messages for registered services.
*   **Remote Agent Management:** Allows for the registration, configuration, and management of remote agents running on different hosts.
*   **Command Execution:** Can dispatch commands to agents and track their status (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `FAILED`) and results.
*   **Script & Path Management:** Provides capabilities to manage executable scripts and configurable paths associated with services.
*   **Asynchronous Operations:** Utilizes `CompletableFuture` and a dedicated thread pool to handle operations like agent and service management in a non-blocking manner, enhancing performance and scalability.
*   **RESTful API:** Exposes a comprehensive set of REST endpoints for interacting with all major resources (Agents, Services, etc.).
*   **Auditing:** Includes dedicated audit logging for critical events like agent creation and deletion.
*   **SSH Command Execution:** Executes remote Linux commands through SSH public-key authentication with host-key verification; no remote WebSocket agent is required.

## Core Components

*   **Agent:** Represents a remote process running on a server, responsible for executing health checks and commands.
*   **Service:** A representation of a monitored application or endpoint.
*   **ServiceHealthStatus:** A record of a health check performed against a service at a specific point in time.
*   **Command:** A task or instruction to be executed by an agent.
*   **Script:** A manageable script that can be executed by an agent.
*   **Path:** A configurable path, likely an API endpoint or resource location, associated with a service.

## Technologies Used

*   **Backend:** Java 21, Spring Boot
*   **Data Persistence:** Spring Data JPA (Hibernate)
*   **Build Tool:** Maven

## Getting Started

### Prerequisites
*   Java 21
*   Apache Maven

### Building the Application
```bash
mvn clean install
```

### Running the Application
```bash
java -jar target/service-pulse-app-0.0.1-SNAPSHOT.jar
```

### SSH Configuration

Remote command execution and agent status checks use the existing Agent `host` value from the database. Configure shared SSH authentication through environment variables; do not store private keys or passphrases in the database or repository.

```powershell
$env:SSH_USERNAME = "service-pulse"
$env:SSH_PRIVATE_KEY_PATH = "C:\secrets\service-pulse\id_ed25519"
$env:SSH_KNOWN_HOSTS_PATH = "C:\secrets\service-pulse\known_hosts"
$env:SSH_PRIVATE_KEY_PASSPHRASE = ""
$env:SSH_PORT = "22"
```

`SSH_KNOWN_HOSTS_PATH` is mandatory. The application rejects unknown host keys instead of disabling host-key checks.

### Runtime flags

The scheduler and remote service-management endpoints can be disabled without changing the application binary. The Settings page displays and saves the two feature flags; saved database values override the environment defaults:

```powershell
$env:HEALTHCHECK_ENABLED = "true"
$env:SERVICE_MANAGEMENT_ENABLED = "true"
$env:HEALTHCHECK_CRON = "0 0/1 * * * *"
```

The scheduler interval is read when the application starts. Service health URLs are stored per server-service association at `/api/server-service-configurations/server/{serverId}`. Existing services continue to use their legacy shared URL until a server-specific configuration is saved.

This flag `backfill-data.populate.enabled`

For deployments using `spring.jpa.hibernate.ddl-auto=none`, run the idempotent migrations at [db/mysql/V2__server_service_configuration.sql](db/mysql/V2__server_service_configuration.sql) and [db/mysql/V3__alert_managemenzt.sql](db/mysql/V3__alert_management.sql) before starting the application. `V2` creates the server-specific configuration tables, migrates existing service URLs, seeds the runtime flags, and removes the legacy `service.healthCheckUrl` column. `V3` creates the alert configuration and alert event tables and seeds the alert-management runtime flag.

Set `JPA_DDL_AUTO=none` when deploying with this script. Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` to the same database used to run the migration. For SSH execution, set `SSH_USERNAME`, `SSH_PRIVATE_KEY_PATH`, and `SSH_KNOWN_HOSTS_PATH`; the application requires a known-hosts file when strict host-key checking is enabled.

The migration is equivalent to these schema changes:

```sql
CREATE TABLE application_configuration (
	configuration_key VARCHAR(100) NOT NULL PRIMARY KEY,
	configuration_value VARCHAR(500) NOT NULL,
	last_updated_dttm DATETIME NULL
);

CREATE TABLE server_service_configuration (
	configuration_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	server_id INT NOT NULL,
	service_id INT NOT NULL,
	health_check_url VARCHAR(255) NULL,
	created_dttm DATETIME NULL,
	last_updated_dttm DATETIME NULL,
	created_source VARCHAR(255) NULL,
	last_updated_source VARCHAR(255) NULL,
	UNIQUE KEY uq_server_service_configuration (server_id, service_id)
);

ALTER TABLE service_health_status ADD COLUMN server_id INT NULL;
ALTER TABLE service_health_status ADD UNIQUE KEY uq_service_health_server (server_id, service_id);

-- Run after copying any existing service.healthCheckUrl values into
-- server_service_configuration.
ALTER TABLE service DROP COLUMN healthCheckUrl;
```