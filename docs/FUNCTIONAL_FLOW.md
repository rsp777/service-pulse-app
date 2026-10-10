# Functional Flow Document: Agentless Service Monitor

## 1. Service Configuration Flow
**Description**: The process of defining which services need to be monitored.
- **Trigger**: Administrator adds a new service entry into the `services` table.
- **Input**: Service Name, URL/IP, Probe Type (HTTP, TCP, SSH).
- **Happy Path**:
    1. Entry is successfully inserted into the PostgreSQL `services` table.
    2. The Poller Engine detects the new entry during its next scheduling cycle.
- **Edge Cases**:
    - Duplicate service names.
    - Invalid URL formats.
    - Unsupported probe types.

## 2. Probing Execution Flow (The Poller)
**Description**: The core loop of checking service health.
- **Trigger**: Scheduled timer (e.g., every 60 seconds).
- **Process**:
    1. **Fetch**: Poller queries `SELECT * FROM services`.
    2. **Dispatch**: For each service, the engine selects the appropriate Probe (HttpProbe, TcpProbe, or SshProbe).
    3. **Execute**:
        - *HTTP*: Send request -> Measure time to first byte -> Check Status Code.
        - *TCP*: Attempt socket connection -> Measure handshake time.
        - *SSH*: Connect via Key -> Run `top/df` -> Parse text.
    4. **Record**: Write result (Value, Metric Type, Timestamp) into the `metrics` table.
- **Happy Path**: All services respond within timeout; status marked as 'Up'.
- **Error Path**:
    - Connection timeout -> Mark as 'Down' -> Log error.
    - SSL Handshake failure -> Mark as 'Down' -> Log 'SSL Error'.

## 3. Dashboard Data Visualization Flow
**Description**: How the user views the health of the system.
- **Trigger**: User opens the Dashboard browser page.
- **Process**:
    1. **Fetch Services**: Frontend calls `/api/services`.
    2. **Fetch Metrics**: Frontend calls `/api/metrics?service_id=X` for each service to get the most recent record.
    3. **Render**: 
        - Status = 'Up' -> Green Indicator.
        - Status = 'Down' -> Red Indicator.
        - Latency > 500ms -> Yellow Warning.
- **Happy Path**: Real-time status updates every X seconds without page reload.
- **Edge Cases**:
    - Database is unreachable -> Dashboard shows "Connection Error" banner.
    - No metrics available for a service -> Show "Pending" or "No Data".

## 4. Alerting/Error State Flow
**Description**: Handling a failure event.
- **Trigger**: A probe returns a 'Down' status or a timeout.
- **Process**:
    1. Poller records the failure in the `metrics` table.
    2. The Dashboard API detects the most recent state is 'Down'.
    3. Dashboard UI highlights the service in Red.
- **Verification for QA**: Change a target URL to a non-existent one -> Verify dashboard turns Red within one polling cycle.
