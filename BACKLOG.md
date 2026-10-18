# Project Backlog: Agentless Service Monitor

## Phase 1: Core MVP (Completed)
- Story 1: Project Structure & Config Management ✅
- Story 2: Java Probe Engine - Core Infrastructure ✅
- Story 3: Java Probe Engine - Network Probes (HTTP & TCP) ✅
- Story 4: Java Probe Engine - SSH System Probe ✅
- Story 5: Dashboard - API Layer ✅
- Story 6: Dashboard - Real-time UI ✅
- Story 7: Containerization & Deployment ✅

## Phase 2: Dynamic Management & Server-Centric View (Completed)
- Story 8: DB-Driven System Configuration ✅
- Story 9: Server Health Metrics UI ✅
- Story 10: Server Resource Consumption View ✅
- Story 11: Remote Service Control (Start/Stop/Restart) ✅

## Phase 3: Advanced Management & Observability (Current)

### Story 12: Advanced Service Lifecycle & Log Management
**Request**: Enable full CRUD for servers and services, and allow custom script paths for control actions and log retrieval.

**Implementation Plan**:
- **Database**: 
    - Update `services` table: add `start_script`, `stop_script`, `restart_script`, and `logs_path`.
- **Backend (Collector)**:
    - Refactor `ControlController` to execute the specific script path defined in the DB.
    - Implement a `LogController` to fetch the last N lines of the `logs_path` via SSH.
- **Backend (Dashboard API)**:
    - Create `DELETE /api/server/[id]` and `DELETE /api/service/[id]`.
    - Create `GET /api/service/[id]/logs`.
    - Create `PATCH /api/service/[id]` to update script/log paths.
- **Frontend (Dashboard UI)**:
    - Add "Delete" actions to Server and Service views.
    - Add a "View Logs" button to the service row that opens a terminal-style log viewer.
    - Add a "Configure" modal for services to edit custom script paths.

**Verification**: 
- Deleting a server removes it from the UI and DB.
- Clicking "Restart" executes a custom shell script instead of a generic command.
- "View Logs" retrieves real-time logs from the remote server's defined path.
