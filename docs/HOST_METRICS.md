# Host metrics

The Dashboard requests `GET /api/servers/{id}/metrics` for the selected registered
host and refreshes every 15 seconds. CPU is utilization across a one-second
sample, memory uses Linux MemAvailable, disk is percent used on the root
filesystem, and uptime is in seconds. The card clears readings on host changes
and shows an explicit unavailable state if collection fails.

Collection uses the existing SSH credentials, timeouts and strict host verification.
The command is fixed and read-only; it requires Linux /proc, POSIX sh, awk and df.
No arbitrary command or destination can be supplied to this endpoint.

No schema migration is required. Responses include serverId, available,
cpuPercent, memoryPercent, diskPercent, uptimeSeconds, sampledAt and message.
Unavailable measurements are null, never fabricated zeroes.

Validate with `mvn "-Dtest=HostMetricsServiceTest,HostMetricsControllerTest" test`
and, from e2e, `npx playwright test tests/host-vitals.spec.ts --project=chromium`.
The automated UI tests use deterministic API fixtures; they do not contact a
managed host or establish live SSH authentication.
