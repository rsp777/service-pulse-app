# Project Backlog: Agentless Service Monitor

## Story 1: Project Structure & Config Management
**Request**: Initialize the project directory structure for both Java (Collector) and TypeScript (Dashboard) and implement a centralized configuration system.
**Implementation Plan**:
- Create a monorepo structure: `/collector` (Java) and `/dashboard` (TypeScript).
- Implement externalized configuration using `.env` files or properties files (for Java, using `application.properties` or environment variables).
- Setup basic build files (Maven/Gradle for Java, `package.json` for TS).
- **Verification**: Project builds without errors; config values are correctly loaded.

## Story 2: Java Probe Engine - Core Infrastructure
**Request**: Build the core scheduling and database connectivity layer for the Poller.
**Implementation Plan**:
- Setup PostgreSQL connection pool (HikariCP) to connect to the `monitor-db` on `ubuntu-dell`.
- Implement a periodic scheduler (e.g., `ScheduledExecutorService`) to trigger probes.
- Create a service to fetch the target list from the `services` table.
- **Verification**: Scheduler runs at defined intervals; DB connectivity is established.

## Story 3: Java Probe Engine - Network Probes (HTTP & TCP)
**Request**: Implement the logic to probe HTTP(S) endpoints and TCP ports.
**Implementation Plan**:
- Implement `HttpProbe`: Uses Java `HttpClient` to measure response time and status codes.
- Implement `TcpProbe`: Uses `java.net.Socket` to check port accessibility.
- Map probe results to the `metrics` table in PostgreSQL.
- **Verification**: Google/GitHub (HTTP) and Postgres (TCP) statuses are correctly recorded in the DB.

## Story 4: Java Probe Engine - SSH System Probe
**Request**: Implement an agentless system metrics gatherer via SSH.
**Implementation Plan**:
- Use a Java SSH library (e.g., JSch or Apache MINA SSHD).
- Execute remote commands (`df`, `free`, `uptime`) on `ubuntu-dell`.
- Parse output to extract CPU/RAM/Disk usage and store in `metrics`.
- **Verification**: System metrics from `ubuntu-dell` are visible in the database.

## Story 5: Dashboard - API Layer
**Request**: Create a backend API to serve monitoring data to the frontend.
**Implementation Plan**:
- Use Next.js API routes or a small Express server.
- Create endpoints: `/api/services` (list all services) and `/api/metrics/:id` (get historical data for a service).
- Implement efficient queries for the "last known status."
- **Verification**: API returns correct JSON data from the PostgreSQL DB.

## Story 6: Dashboard - Real-time UI
**Request**: Build a responsive dashboard to visualize the status of monitored services.
**Implementation Plan**:
- Use Tailwind CSS for a clean, professional look.
- Implement a "Status Grid" showing current Up/Down state and latency.
- Implement simple charts (e.g., using Chart.js or Recharts) for latency over time.
- Add auto-refresh/polling to simulate real-time updates.
- **Verification**: Dashboard correctly reflects the data being written by the Java poller.

## Story 7: Containerization & Deployment
**Request**: Prepare the application for deployment using Docker.
**Implementation Plan**:
- Create `Dockerfile` for the Java Collector (multi-stage build).
- Create `Dockerfile` for the Next.js Dashboard.
- Create a `docker-compose.yml` to orchestrate the Poller, Dashboard, and Database.
- Ensure all configs are passed via environment variables.
- **Verification**: `docker-compose up` launches the entire system successfully.
