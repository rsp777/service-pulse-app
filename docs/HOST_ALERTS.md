# Host alerts and notification refresh

Host checks run 15 seconds after startup and then every 60 seconds after the
previous scan finishes. Configure HOST_MONITOR_INITIAL_DELAY_MILLIS and
HOST_MONITOR_INTERVAL_MILLIS to change these intervals.

Checks use authenticated SSH reachability with the existing credential and
host-verification configuration. ONLINE means SSH is reachable and authenticated;
OFFLINE includes network, authentication, and trust failures. Host status and
last-check timestamps are updated without rewriting inventory associations.

Server rules use SERVER_STATUS with ONLINE, OFFLINE or UNKNOWN (UP/DOWN aliases
are supported), or RESOURCE_USAGE with CPU:80, MEMORY:85 or DISK:90 thresholds.
Numeric comparisons support GREATER_THAN, LESS_THAN, EQUALS and NOT_EQUALS.
CPU uses a one-second sample; disk describes the root filesystem. Resource
samples are collected only for reachable hosts with enabled resource rules.
Missing telemetry leaves an existing resource alert open rather than claiming
recovery. Repeated matching samples reuse the active event; recovery resolves
it and creates an unread resolution notification.

Service samples evaluate only service rules. Legacy STATUS and RESPONSE_TIME
service rules remain supported. Server events have no service ID.

The persisted healthcheck.enabled flag disables host probes. The persisted
alert-management.enabled flag disables alert evaluation and resource collection,
while host reachability still updates. Environment/property defaults apply
when the corresponding persisted flag is absent.

The notification badge and dropdown refresh every 10 seconds on every page.
Periodic polls do not overlap, failed polls retain the last unread state, and
outdated responses cannot undo a newer read acknowledgment.

No schema migration is required. Merge the host metrics prerequisite from
issue 33 before issue 34. Unit/MockMvc and deterministic Chromium tests cover
host isolation, recovery, thresholds, flags and browser polling. Live MySQL,
SSH, distributed concurrent evaluation and deployment require environment
validation; this change does not add a distributed scheduler lock.
