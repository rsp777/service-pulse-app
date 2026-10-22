# Antigravity Workspace & Developer Context

## 1. Developer Profile & Engineering Philosophy
- **Developer:** Ravindra Singh Pawar
- **Role:** Software Engineer, DevOps Engineer, and QA Automation Specialist (~6.5+ years of IT experience).
- **Core Technical Stack:**
  - **Backend & Runtime:** Java (JDK 21+ with Virtual Threads capability), Spring Boot 3.1.5 Microservices, GraalVM Native Image, Relational Databases (Active: MySQL 8.0; Target/Roadmap: PostgreSQL).
  - **Infrastructure & Orchestration:** Linux Server Administration, Docker, Docker Compose, Docker Swarm, Kubernetes (GKE), Tailscale Mesh VPN, HashiCorp Vault.
  - **CI/CD & GitOps:** Jenkins, GitHub Actions, GitOps delivery patterns, automated configuration drift detection.
  - **Observability:** OpenTelemetry, Micrometer, Prometheus, Spring Boot Actuator.
  - **QA & Automation:** Playwright with TypeScript, Model Context Protocol (MCP) integrations, automated test generation.
- **Architectural Preferences:**
  - Strongly prefers **self-hosted platforms**, **terminal-first developer tools**, and **agentless architectures** where possible.
  - Prefers **local Small Language Model (SLM) deployments via Ollama** and **MCP** over expensive third-party cloud SaaS wrappers.
  - **IMPORTANT EXCLUSION:** Do **NOT** suggest or generate Server-Driven UI (SDUI) engines, HTMX, or Thymeleaf architectures (explicitly discarded).
  - Emphasizes stateless container design, zero-trust networking (Tailscale), and strict memory/resource hygiene in Java.

---

## 2. Project Context: `service-pulse-app`
**Overview:** An open-source Linux Operations, Application Performance Monitoring (APM), and agentless remote SSH execution platform designed to run cleanly in Docker Compose, Docker Swarm, and multi-replica Kubernetes clusters.

### Current UI & Operational Layout
- **Navigation Sections:** Dashboard, Servers, Services, Commands, Alerts, System Logs, Settings.
- **Dashboard Viewport:**
  - **Top Summary Cards:** Registered Servers, Services Online, Active Alerts, SSH Execution Engine status.
  - **Active Server Context:** Selects the target host (e.g., connected over Tailscale IPs like `100.x.y.z`), showing OS and online status.
  - **SSH Remote Execution Panel:** Executes preconfigured or custom shell commands on the active server context.
  - **Monitored Services Panel:** Lists pinned services per server with lifecycle controls (`Refresh`, `Start All`, `Stop All`, `Restart All`, `+ Add service`) and row-level actions (`Start`, `Stop`, `Restart`, `Logs`, `Edit`, `Delete`).
  - **Bottom Console (`CONSOLE // APM OUTPUT`):** Dark terminal output pane streaming command execution results and service lifecycle logs with `Copy Output` and `Clear` actions.

---

## 3. Current Implementation State vs Target Architecture

### A. Database Layer: Active MySQL 8.0 & Target PostgreSQL Schema
- **Current Active Database (MySQL 8.0):**
  - Driver: `com.mysql:mysql-connector-j` (8.0.33)
  - Hibernate Dialect: `org.hibernate.dialect.MySQLDialect`
  - Migration Tool: **Liquibase** (`src/main/resources/db/changelog/db.changelog-master.xml` invoking `sql/V1__init.sql`)
  - Active Core Tables (with `BIGINT AUTO_INCREMENT` PKs):
    - `server`: Stores managed target servers (`id`, `hostname`, `ip_address`, `os_type`, `status`, `last_health_checked`, `ssh_port`, `ssh_username`, `ssh_key_id`).
    - `service`: Catalog of monitored services (`id`, `service_name`).
    - `server_service`: Many-to-many junction between servers and services.
    - `server_service_configuration`: Runtime configuration (`actuator_port`, `context_path`, `health_check_url`, `logs_path`, `scripts_path`, `status`).
    - `service_health_status`: Historical and current health telemetry (`id`, `service_id`, `status`, `response_time`, `error_message`, `time_stamp`).
    - `command`: Registered remote SSH commands (`id`, `name`, `description`, `parameters`, `status`, `result`).
    - `health_check`, `http_methods`, `path`, `script`, `server_path`, `path_script`.
    - `alert_configuration`, `alert_event`, `alert_condition_type`, `alert_condition_mapping`, `alert_event_log`.
    - `application_configuration`, `ui_actions`.
- **Server SSH Configuration & Connectivity (Issue #26 :white_check_mark:):**
  - Added `ssh_port` (default 22), `ssh_username` (default 'ubuntu'), and `ssh_key_id` to `server` entity and Liquibase changeset `2-add-server-ssh-fields`.
  - Removed manual static status select from Add/Edit Server modal, dynamically relying on health polling.
  - Implemented live SSH connectivity testing via `POST /api/servers/test-connection` and `POST /api/servers/{id}/test-connection` with real-time UI status feedback.
  - Certified with 13 JUnit 5 unit tests and 30 cross-browser Playwright E2E tests (+ve & -ve).
- **Service Application Health Check URL & Live Configuration (Issue #27 :white_check_mark:):**
  - Added `healthCheckUrl`, `actuatorPort`, and `contextPath` input controls to the Service Application modal (`index.html`, `services.js`).
  - Added automatic Spring Boot Actuator URL derivation helper with dynamic binding on port/context path/server selection changes and manual override support.
  - Added backend endpoint `GET /api/server-service-configurations` in `ServerServiceConfigurationController.java` to fetch all runtime configurations cross-server.
  - Updated Service Registry table in `services.js` to populate the `HEALTH CHECK URL` column with live configuration data across all servers.
  - Updated `dashboard.js` to route live actuator health and version queries through the secure backend proxy (`GET /api/services/runtime-status?url=...`), preventing browser CORS errors.
  - Certified with 6 JUnit 5 unit tests and 27 cross-browser Playwright E2E tests across Chromium, Firefox, and WebKit (+ve & -ve).
- **Service Registry Live Status & Framework/Type Badges (Issue #28 :white_check_mark:):**
  - Added `serviceType` to `ServiceResponseDto` with backwards-compatible canonical constructor.
  - Updated `ServiceMapper` to map `serviceType` bi-directionally between `Service` entity and `ServiceResponseDto`.
  - Added `.type-badge` styles in `styles.css` for `spring-boot`, `systemd`, and `other`.
  - Updated `loadAllServicesView()` in `services.js` to render `Type` and live `Status` pill badge columns with pulse dots.
  - Certified with 7 JUnit 5 unit tests in `ServiceMapperTest` and 30 cross-browser Playwright E2E tests across Chromium, Firefox, and WebKit (+ve & -ve).
- **Roadmap Target (PostgreSQL & UUID PKs):**
  - Planned migration for cloud-native distributed scaling: migrate primary keys from `BIGINT AUTO_INCREMENT` to `UUID` to prevent ID collisions in distributed multi-region sync.
  - PostgreSQL schema definitions for `ssh_key_pairs`, `server_key_assignments`, and `shedlock`.

---

### B. SSH Security Engine: Keyless Ephemeral Architecture (Implemented :white_check_mark:)
The application has fully evolved from static disk-based SSH keys to **Keyless SSH Authentication with Ephemeral Rotating Keys and OpenSSH Certificates** (Epic #16, Sub-Issues #17–#23):
1. **Pluggable `SshCredentialProvider` SPI (#17):**
   - Decoupled `SshCommandService` from physical filesystem paths.
   - Dual authentication modes: `STATIC` (legacy disk fallback) and `CA_CERTIFICATE` (modern keyless CA mode).
2. **Ephemeral In-Memory Keypair Generation & Zeroization (#18):**
   - Cryptographic keypairs (native Java 21 `Ed25519` via RFC 8410, and RSA-4096) generated strictly in memory (`DefaultEphemeralKeyPairGenerator`). Zero disk writes.
   - Public keys encoded to OpenSSH wire format (`OpenSshKeyEncoder` via RFC 4253).
   - In-memory key zeroization on disposal (`EphemeralKeyPair.close()`) and sanitized `toString()` output.
3. **SSH Certificate Authority (CA) Integration (#19):**
   - `VaultSshCertificateAuthorityClient` requests signed OpenSSH User Certificates from HashiCorp Vault SSH Secrets Engine (`/v1/ssh/sign/servicepulse-runner`).
   - `OpenSshCertificateParser` parses PROTOCOL.certkeys binary wire blobs into `OpenSshCertificate`.
   - `CertificateKeyProvider` seamlessly loads certificate and ephemeral private key into `sshj`.
   - Auto-provisioned Vault SSH CA configured in `docker-compose.yml` (`service-pulse-vault`, `service-pulse-vault-init`).
4. **Proactive In-Memory Rotation Engine (#20):**
   - `SshCertificateRotationEngine` (`SmartLifecycle`) proactively schedules rotation at **75% of cert TTL** (e.g. 45 min on a 60 min cert).
   - Atomic lock-free hot swapping via `AtomicReference<ActiveSshCredential>` in `CaCertificateCredentialProvider`. In-flight sessions and log streams proceed without interruption.
   - Exponential backoff retry on CA communication failures.
5. **Bidirectional Cryptographic Trust & Strict Host-Key Policy (#21):**
   - Remote target servers configure `TrustedUserCAKeys /etc/ssh/user_ca.pub` once; zero `authorized_keys` fleet sprawl.
   - `HostCertificateVerifier` validates target servers presenting OpenSSH Host Certificates against the trusted Host CA (`@cert-authority`).
   - `PromiscuousVerifier` is strictly restricted to development/testing profiles.
6. **Actuator Health, Micrometer Metrics & Audit Logging (#22):**
   - Spring Boot Actuator `/actuator/health` component `sshCertificate` reports `UP`, `OUT_OF_SERVICE`, or `DOWN` with non-sensitive metadata (serial, keyId, remaining TTL seconds, rotation count).
   - Micrometer Prometheus gauges (`ssh.cert.ttl.seconds`), counters (`ssh.cert.rotations.total`), and timers (`ssh.ca.sign.duration`).
   - High-integrity structured security audit logs (`SshSecurityAuditLogger`) recording SSH events without exposing secrets.
7. **Automated End-to-End Test Suite (#23):**
   - `KeylessSshIntegrationTest` verifies 5 scenarios including zero-downtime rotation, untrusted CA rejection, and expired certificate handling.
8. **SSH Credentials & Dynamic Server Probing (#26):**
   - Added SSH credentials, Vault CA integration, dynamic status probing, and connection test endpoint (`POST /api/servers/{id}/test-connection`).
   - Merged to `dev`, closed Issue #26.
9. **Actuator Health Check URL & Multi-Server Association (#27):**
   - Dynamic Actuator health check URL resolution with port and context path overrides in service modal.
   - Multi-server service configuration query endpoint `GET /api/server-service-configurations`.
   - Certified with 6 JUnit 5 tests and 27 Playwright cross-browser tests. Merged to `dev`, closed Issue #27.
10. **Global Service Registry Type & Live Status Badges (#28):**
   - Exposed `serviceType` across DTOs and Mappers with CSS styling (`.spring-boot`, `.systemd`, `.other`).
   - Added `TYPE` badge and live `STATUS` pill badge (`• UP` / `• DOWN`) with pulse dots in `loadAllServicesView()`.
   - Certified with 11 JUnit 5 tests and 30 Playwright cross-browser tests. Merged to `dev`, closed Issue #28.
11. **Preconfigured Command Context Selector & Status Tracking (#29):**
   - Target server context selector `#command-target-server` dynamically synced with active server state.
   - Enforced server selection validation with user prompt notice before execution.
   - Reusable command execution via `POST /api/commands/execute?commandId=N`, updating execution timestamp, exit code (`0` for COMPLETED, `1` for FAILED), and output streaming to dashboard console.
   - Certified with 6 JUnit 5 tests (`CommandControllerTest`) and 21 Playwright cross-browser tests covering positive (+ve) and negative (-ve) paths. Merged to `dev`, closed Issue #29.

---

## 4. Unimplemented Architecture & Concrete Action Plan

The following architectural components from the roadmap are currently **unimplemented** in the codebase. Below is the detailed gap analysis and execution blueprint for each:

### Gap 1: Distributed Scheduling & Multi-Replica Pod Coordination (ShedLock & Virtual Threads)
- **Current State:**
  - Scheduled tasks (`healthcheck.cron`, `alert.cron`, and certificate rotation) run on local single-instance Spring thread pools. If multiple container replicas run in Kubernetes or Swarm, duplicate health checks and alerts will collide.
  - Virtual Threads are supported by the GraalVM/Java 21 runtime but are not explicitly enabled in Spring Boot configuration.
- **Action Plan:**
  1. Add dependencies to `pom.xml`:
     ```xml
     <dependency>
         <groupId>net.javacrumbs.shedlock</groupId>
         <artifactId>shedlock-spring</artifactId>
         <version>5.10.0</version>
     </dependency>
     <dependency>
         <groupId>net.javacrumbs.shedlock</groupId>
         <artifactId>shedlock-provider-jdbc-template</artifactId>
         <version>5.10.0</version>
     </dependency>
     ```
  2. Add Liquibase migration for the `shedlock` coordination table in `src/main/resources/db/changelog/sql/`:
     ```sql
     CREATE TABLE IF NOT EXISTS shedlock (
         name VARCHAR(64) NOT NULL PRIMARY KEY,
         lock_until TIMESTAMP(3) NOT NULL,
         locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
         locked_by VARCHAR(255) NOT NULL
     );
     ```
  3. Create `ShedLockConfiguration.java` providing `LockProvider` using `new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder().withJdbcTemplate(jdbcTemplate).usingDbTime().build())` and annotate with `@EnableSchedulerLock(defaultLockAtMostFor = "10m")`.
  4. Annotate schedulers with `@SchedulerLock(name = "healthcheck_poll", lockAtLeastFor = "30s", lockAtMostFor = "50s")`.
  5. Enable Virtual Threads in `application.properties`:
     ```properties
     spring.threads.virtual.enabled=true
     ```

---

### Gap 2: Workload Discovery & Classification Heuristics
- **Current State:**
  - Monitored services must be manually created via the UI/API and associated with servers. The system does not automatically inspect remote Linux systems to classify workloads.
- **Action Plan:**
  1. Implement `com.pawar.todo.amt.discovery.LinuxWorkloadDiscoveryService`:
     - Executes non-invasive SSH commands over `SshCommandService`:
       - `systemctl list-units --type=service --state=running --no-pager --all`
       - `ss -tulpn` (identifying listening TCP/UDP sockets and binding PIDs)
       - `ps -eo pid,user,args` and `/proc/<pid>/cmdline`
  2. Implement `ServiceClassificationHeuristicEngine`:
     - **`CUSTOM_APP`:** Services whose unit files reside in `/etc/systemd/system/*.service` or running under non-system UIDs (`UID >= 1000`).
     - **`UNMANAGED_PROCESS`:** Standalone `nohup java -jar` processes detected via `/proc/<pid>/cmdline` not belonging to a systemd slice. Computes a deterministic identity signature: `workingDirectory + jarBaseName + listeningPort`.
     - **`NETWORK_INFRA`:** Standard infrastructure daemons in `/lib/systemd/system/` bound to network ports (e.g., Nginx, Redis, MySQL, Docker).
     - **`OS_SYSTEM`:** Low-level OS daemons (`systemd-*`, `dbus`, `getty@`, `udev`) hidden from the APM view by default unless reporting a failed status.
  3. Implement "Promote to Systemd": Expose an API/UI action that generates a hardened systemd unit file on the target server for unmanaged `nohup` Java applications.

---

### Gap 3: Remote Docker Engine & Kubernetes Cluster Monitoring
- **Current State:**
  - APM only collects OS-level process metrics and HTTP health endpoints over SSH; Docker daemon API and Kubernetes Cluster API integrations are not implemented.
- **Action Plan:**
  1. Add dependencies to `pom.xml`:
     ```xml
     <dependency>
         <groupId>com.github.docker-java</groupId>
         <artifactId>docker-java-core</artifactId>
         <version>3.3.4</version>
     </dependency>
     <dependency>
         <groupId>com.github.docker-java</groupId>
         <artifactId>docker-java-transport-httpclient5</artifactId>
         <version>3.3.4</version>
     </dependency>
     <dependency>
         <groupId>io.fabric8</groupId>
         <artifactId>kubernetes-client</artifactId>
         <version>6.10.0</version>
     </dependency>
     ```
  2. Implement `DockerClientService`:
     - Configured as a strict Spring singleton bean with capped connection pooling (`maxConnections: 50`) and timeouts.
     - Connects securely across nodes over Tailscale TCP (`tcp://100.x.y.z:2375` or TLS `2376`).
     - Streams container logs and resource metrics wrapping `ResultCallback.Adapter` inside `try-with-resources` to eliminate memory leaks.
  3. Implement `KubernetesClusterMonitoringService`:
     - Uses `io.fabric8:kubernetes-client` with a dedicated read-only `ServiceAccount` (`pulse-monitor`).
     - Listens to Pod lifecycle events, pod crash loops (`CrashLoopBackOff`), and deployment rollouts via Kubernetes Informers/Watchers.

---

### Gap 4: AIOps & Model Context Protocol (MCP) Integration
- **Current State:**
  - No Model Context Protocol tools or local LLM/Ollama integrations are currently present in the codebase.
- **Action Plan:**
  1. Implement Spring-based MCP Server exposing operational tools:
     - `@McpTool` `getServerHealth(serverId)`
     - `@McpTool` `getServiceLogs(serverId, serviceId, lines)`
     - `@McpTool` `executeDiagnosticCommand(serverId, commandName)`
     - Human-in-the-loop interceptor requiring explicit user confirmation before executing any state-modifying action (e.g., service restart, container reboot).
  2. Implement `OllamaLogAnomalyAnalyzer`:
     - Integrates with local Ollama instance (`http://localhost:11434/api/generate`) using lightweight SLMs (`qwen2.5-coder:7b` or `llama3.2`).
     - **Security Sanitizer Pre-Filter:** Mandatory regex pipeline scrubbing JWT tokens, API keys, private keys, passwords, and PII from stack traces before passing logs to the inference prompt.

---

## 5. Coding Rules for Antigravity Agents
1. **No SDUI / HTMX / Thymeleaf:** Never generate Thymeleaf templates or HTMX attributes. Keep backend APIs clean (REST / WebSockets / SSE) and decoupled from server-rendered HTML engines.
2. **Stateless by Default:** Never write state, temporary scripts, or SSH keys to the local container filesystem. All cryptographic operations must occur strictly in RAM with zeroization on disposal.
3. **Resource Safety:** Always wrap network streams, SSH sessions, and Docker callbacks in `try-with-resources` and enforce strict timeouts and output buffer caps.
4. **Security First:** Never log decrypted keys, raw tokens, or unredacted environment variables. Disallow `PromiscuousVerifier` in production.
5. **Database Consistency:** Maintain active support for MySQL 8.0 schema via Liquibase changelogs while preserving modular entity models ready for future PostgreSQL migration.