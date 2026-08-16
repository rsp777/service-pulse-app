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