# `service-pulse-app` — GitHub Issues Tracker & Antigravity Agent Directive

## Part 1: GitHub Issues (Major Gaps, Minor Bugs & Feature Discrepancies)

### 🔴 Priority 1: Major Architectural & Functional Gaps

#### Issue #1: `Server Host` Modal Lacks SSH Authentication, Port, and Username Configuration
- **Labels:** `bug`, `critical`, `ssh-engine`, `frontend`, `backend`
- **Current Behavior:**
  - In the **Server Host** modal (`Servers` -> `+ Add server` / `Edit`), the only fields present are `Server Name / Hostname *`, `IP Address or DNS *`, `Operating System`, and a manual `Status` dropdown (`ONLINE` / `OFFLINE`).
- **Problem Statement:**
  1. An agentless SSH execution engine cannot connect to a remote Linux host without an **SSH Port** (default `22`), **SSH Username** (e.g., `ubuntu`, `root`), and **Authentication Material** (assigned SSH Key Pair or password).
  2. `Status` (`ONLINE` / `OFFLINE`) should not be a manual user-editable dropdown in the form—it must be dynamically computed by the backend reachability poller.
- **Acceptance Criteria:**
  - [ ] Add `sshPort` (number, default `22`), `sshUsername` (string, default `ubuntu`), and `sshKeyId` (dropdown selector) to the `Server` entity, REST DTOs, and modal form.
  - [ ] Remove the manual `Status` dropdown from the Add/Edit Server modal.
  - [ ] Add a **"Test SSH Connection"** button inside the modal to verify credentials before saving.

---

#### Issue #2: `Service Application` Modal Missing `Health Check URL` Input Despite Showing in Dashboard & Registry
- **Labels:** `bug`, `data-mismatch`, `services`
- **Current Behavior:**
  - The **Dashboard** displays `Health URL: Not configured` and `Version: Unknown` under `auth-service`.
  - The **Service Registry** table (`Services` tab) has a dedicated `HEALTH CHECK URL` column (currently displaying `-`).
  - However, the **Service Application** modal only provides inputs for: `Target Server *`, `Service Identifier / Name *`, `Service Framework / Type` (`Other Service` / `Spring Boot`), `LOGS Directory Path` (`/opt/apps/logs`), and `Lifecycle Scripts Directory Path` (`/opt/apps/scripts`).
- **Problem Statement:**
  - Users have no input field in the UI to configure or edit the `Health Check URL` (e.g., `http://localhost:8080/actuator/health`) or expected port, making it impossible to populate the `HEALTH CHECK URL` column or resolve `Health URL: Not configured` and `Version: Unknown` on the Dashboard.
- **Acceptance Criteria:**
  - [ ] Add `Health Check URL / Actuator Endpoint` and `Service Port` input fields to the **Service Application** modal.
  - [ ] When `Service Framework / Type` is set to `Spring Boot`, dynamically offer an Actuator base path helper and parse `/actuator/info` and `/actuator/health` to populate `Version` and `Status` on the Dashboard.

---

#### Issue #3: `Service Registry` Missing Live `STATUS` and `FRAMEWORK / TYPE` Columns
- **Labels:** `enhancement`, `ui`, `services`
- **Current Behavior:**
  - In the **Services** tab (`Service Registry`), the table columns are `ID`, `SERVICE NAME`, `SERVER`, `HEALTH CHECK URL`, and `ACTIONS` (`View`, `Edit`, `Delete`).
- **Problem Statement:**
  - Unlike `Server Inventory` (which shows `• ONLINE` / `• OFFLINE` pill badges), the `Service Registry` does not display whether a service is `UP` or `DOWN`, nor does it show its `Service Framework / Type` (`Spring Boot`, `Systemd`, `Other Service`).
- **Acceptance Criteria:**
  - [ ] Add a `TYPE` badge column (`Spring Boot`, `Systemd`, `Other Service`) to the `Service Registry` table.
  - [ ] Add a live `STATUS` (`• UP` / `• DOWN`) pill badge column to the `Service Registry` table.

---

#### Issue #4: `Preconfigured Commands` Execution Lacks Target Server Context Selector
- **Labels:** `bug`, `ux`, `commands`
- **Current Behavior:**
  - In the **Commands** tab (`Preconfigured Commands`), row `#3` (`df` / `-h`) has a green `Run` button directly in the table and a `STATUS` column permanently showing `CREATED`.
- **Problem Statement:**
  1. On the `Commands` page, there is no indicator or dropdown showing *which server* clicking `Run` will execute `df -h` against.
  2. A reusable command template shouldn't have a static `CREATED` status column—users need `Last Run Status` (e.g., `SUCCESS (Exit 0)` / `FAILED`) or `Last Executed At`.
  3. In the **SSH Command** modal, splitting `df` (`Command Name *`) and `-h` (`Command String / Parameters *`) is ambiguous. `Command Name` should be a human-readable title (e.g., `"Check Disk Space"`), while `Command String` should hold the full command (`df -h`).
- **Acceptance Criteria:**
  - [ ] Refactor modal fields into `Template Name` (e.g., `"Check Disk Space"`), `Command String` (e.g., `df -h`), and `Description`.
  - [ ] Add a target server selector (or prompt) when clicking `Run` on the Commands page and stream the execution output to the console.
  - [ ] Replace the static `CREATED` column with `Last Exit Code` / `Last Executed At`.

---

#### Issue #5: State Desynchronization Between `Live Service Log Stream` and `DOWN` Service Badge
- **Labels:** `bug`, `state-management`, `logs`
- **Current Behavior:**
  - On the Dashboard, `auth-service` shows `Status: • DOWN`.
  - Clicking `Logs` opens the **Live Service Log Stream** modal (`TAIL -F // SERVICE LOGS`), which actively streams live Spring Kafka logs (`org.springframework.kafka.KafkaListenerEndpointContainer...`).
- **Problem Statement:**
  - The JVM process for `auth-service` is running and actively writing logs, yet the Dashboard health status reports `• DOWN` because `Health URL` is unconfigured and the status poller lacks a fallback check.
- **Acceptance Criteria:**
  - [ ] Implement tiered health evaluation:
    1. If `Health Check URL` is configured, probe HTTP `200 OK` (or Spring Actuator `"status": "UP"`).
    2. If `Health Check URL` is blank, fallback to process/systemd status verification (`systemctl is-active` or `pgrep -f`) so active services are not falsely marked `DOWN`.

---

### 🟡 Priority 2: Minor UI/UX Bugs & Polish Issues

#### Issue #6: `Live Service Log Stream` Modal Missing Controls Present in `System Logs` & Top Line Clipping
- **Labels:** `enhancement`, `ui`, `logs`
- **Current Behavior:**
  - The **Live System Logs** page includes a log-level filter dropdown (`INFO`), `Auto-scroll` checkbox, `Pause Stream`, and `Clear` buttons.
  - The **Live Service Log Stream** modal only has a `Close Stream` button, and the first line of the terminal output is vertically clipped at the top border of the dark container.
- **Acceptance Criteria:**
  - [ ] Add `Auto-scroll`, `Pause Stream`, `Copy`, `Clear`, and a text/level filter to the **Live Service Log Stream** modal header.
  - [ ] Add `padding-top: 12px` inside the modal's terminal `<pre>` container to fix the top-line text clipping.
  - [ ] Ensure clicking `Close Stream`, `x`, or pressing `Escape` explicitly closes the JS `EventSource` / `WebSocket` and terminates the remote SSH `tail -f` process on the backend.

---

#### Issue #7: Service Lifecycle Action Buttons (`Start` / `Stop`) Are Not State-Aware
- **Labels:** `ux`, `frontend`, `dashboard`
- **Current Behavior:**
  - On the Dashboard, `auth-service` is marked `• DOWN`, yet `Stop` and `Restart` are clickable and active.
- **Acceptance Criteria:**
  - [ ] Disable/dim the `Stop` button when a service is `DOWN`, and disable/dim the `Start` button when a service is `UP`.
  - [ ] Add a confirmation prompt (`confirm()`) before executing destructive actions (`Delete`, `Stop`, `Stop All`, `Restart All`).

---

#### Issue #8: Redundant Server Title & Unutilized Whitespace in Dashboard Server Card
- **Labels:** `ui`, `polish`, `dashboard`
- **Current Behavior:**
  - In the middle-left card on the Dashboard, `auth-wms` is displayed as the card title, and `auth-wms (100.87.125.44)` is repeated inside the `Active Server Context` dropdown directly below it, leaving the bottom 40% of the card empty.
- **Acceptance Criteria:**
  - [ ] Consolidate the card header with the `Active Server Context` selector.
  - [ ] Populate the lower half of the card with live host vitals fetched over SSH (`CPU %`, `RAM Usage`, `Disk %`, `Uptime`).

---

#### Issue #9: `Alerts` & `Notification Bell` Implementation Gaps
- **Labels:** `feature-gap`, `alerts`
- **Current Behavior:**
  - The `Alert & Condition Management` page has three tabs (`Alert Rules`, `Triggered Events Log`, `Condition Types & Mappings`) in an empty state, alongside a notification bell icon in the top-right header across all pages.
- **Acceptance Criteria:**
  - [ ] Wire the `+ Add alert rule` modal to evaluate real conditions (`Service Status == DOWN`, `Server Status == OFFLINE`, `Disk Usage > threshold`).
  - [ ] Connect the top-right notification bell icon to display an unread badge count and dropdown list of recent `Triggered Events`.

---

## Part 2: Antigravity Agent Execution Directive

### Context & Tech Stack Constraints
You are working on `service-pulse-app` (v0.0.4, Spring Boot 3).
- **Backend:** Java 21+ / Spring Boot 3 (`@RestController`, SSE via `SseEmitter`, stateless SSH execution engine).
- **Frontend:** Native Vanilla JavaScript (ES6+), HTML5, and CSS3 located in `src/main/resources/static/`.
- **STRICT ARCHITECTURAL RULE:** Do **NOT** introduce Server-Driven UI (SDUI), Thymeleaf, HTMX, or any Node/npm SPA framework (React/Vue/Angular). All UI updates must use clean Vanilla JS (`fetch`, `EventSource`, DOM APIs) and existing CSS styles.

---

### Phase 1: Codebase Discovery & Audit (Execute First)
Before modifying any files, scan the repository and output a brief **Technical Audit Report** covering:
1. **Backend Entities & Schema:** Inspect all JPA entities/models (`Server`, `ServiceApplication`, `SshCommand`, `AlertRule`, etc.) and database schema files. List which fields already exist in the backend vs. which are missing from REST DTOs and frontend modals.
2. **SSH & Lifecycle Engine:** Inspect how SSH connections, `Preconfigured Commands`, service lifecycle scripts (`/opt/apps/scripts` for `Start`, `Stop`, `Restart`), and `Live Service Log Stream` (`tail -f` on `/opt/apps/logs`) are implemented.
3. **Health & Status Polling:** Inspect how `Server` status (`ONLINE`/`OFFLINE`) and `Service` status (`UP`/`DOWN`) are computed. Pinpoint why `auth-service` shows `DOWN` on the Dashboard while streaming live logs in the modal.
4. **Frontend JS Wiring:** Inspect the Vanilla JS files in `src/main/resources/static/` to map how modals, table renders, SSE streams (`/api/system/logs/stream` and service log streams), and tabs (`Alerts`, `Settings`) are bound.

---

### Phase 2: Sequential Implementation Plan
Execute the following steps one by one, verifying compilation and UI bindings after each step:

#### Step 1: Fix `Server Host` Entity, API & Modal (Issue #1)
- Add `sshPort` (default `22`), `sshUsername` (default `ubuntu`), and SSH key/credential binding to the `Server` entity, DTOs, and the **Server Host** modal (`+ Add server` / `Edit`).
- Remove the manual `Status` (`ONLINE`/`OFFLINE`) dropdown from the modal and replace it with an automated backend SSH reachability check (`Test Connection` button + background ping).

#### Step 2: Fix `Service Application` Modal, Health Probing & Registry Table (Issues #2, #3, #5)
- Add `healthCheckUrl` and `servicePort` fields to the **Service Application** modal (`+ Add service` / `Edit`).
- Implement tiered health checking in the backend:
  - If `healthCheckUrl` is configured, probe HTTP status (and parse `/actuator/health` & `/actuator/info` when `Service Framework / Type` is `Spring Boot`).
  - If `healthCheckUrl` is empty, fallback to checking the process/systemd status over SSH (`pgrep -f` or status script) so active services are accurately marked `UP`.
- Update the **Service Registry** table (`Services` tab) to render `TYPE` and live `STATUS` (`• UP` / `• DOWN`) pill badges.

#### Step 3: Upgrade `Live Service Log Stream` Modal & Stream Cleanup (Issue #6)
- Fix the top padding inside the `TAIL -F // SERVICE LOGS` modal terminal container so the first line is not clipped.
- Add `Auto-scroll`, `Pause Stream`, `Copy`, `Clear`, and log filtering controls to the `Live Service Log Stream` modal to match `Live System Logs`.
- Ensure closing the modal (`Close Stream`, `x`, or `Escape`) closes the client `EventSource` and kills the remote SSH `tail -f` stream on the backend.

#### Step 4: Fix `Preconfigured Commands` Execution & Semantics (Issue #4)
- Clarify `Command Name` (template title) vs. `Command String` (full executable command + arguments) in the **SSH Command** modal and table.
- Add a target server selector when running a preconfigured command from the `Commands` tab and stream the output to the console.
- Replace the static `CREATED` status column with `Last Run Status` / `Last Executed At`.

#### Step 5: Dashboard UX Polish, Host Vitals & Alerts Wiring (Issues #7, #8, #9)
- Make `Start` / `Stop` buttons state-aware (disable `Stop` when `DOWN`; disable `Start` when `UP`) and add confirmation dialogs for `Delete`, `Stop All`, and `Restart All`.
- Remove the duplicate hostname header in the Dashboard server card and display compact Host Vitals (`CPU %`, `RAM`, `Disk`, `Uptime`) in the card's lower section.
- Wire the `Alert & Condition Management` tabs and top-right notification bell to backend event records.