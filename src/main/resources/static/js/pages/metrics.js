'use strict';

// Issue #33: One non-overlapping telemetry poll, scoped to the selected host.
let hostMetricsTimer = null;
let hostMetricsAbort = null;
let hostMetricsSelection = 0;

function renderHostMetrics(metrics, message) {
    const percent = value => Number.isFinite(value) ? value.toFixed(1) + '%' : '—';
    $('metrics-cpu-percent').textContent = 'CPU: ' + percent(metrics?.cpuPercent);
    $('metrics-mem-percent').textContent = 'Memory: ' + percent(metrics?.memoryPercent);
    $('metrics-disk-percent').textContent = 'Disk (/): ' + percent(metrics?.diskPercent);
    const seconds = metrics?.uptimeSeconds;
    $('metrics-uptime').textContent = 'Uptime: ' + (Number.isFinite(seconds)
        ? Math.floor(seconds / 86400) + 'd ' + Math.floor(seconds % 86400 / 3600) + 'h ' + Math.floor(seconds % 3600 / 60) + 'm'
        : '—');
    $('metrics-status').textContent = message;
}

function selectHostMetrics(serverId) {
    clearTimeout(hostMetricsTimer);
    if (hostMetricsAbort) hostMetricsAbort.abort();
    const selection = ++hostMetricsSelection;
    renderHostMetrics(null, serverId ? 'Loading metrics...' : 'Select a server to view metrics.');
    if (!serverId) return;
    async function refresh() {
        if (selection !== hostMetricsSelection) return;
        hostMetricsAbort = new AbortController();
        try {
            const metrics = unwrap(await apiRequest(API.servers.metrics(serverId),
                { silent: true, signal: hostMetricsAbort.signal }));
            if (selection !== hostMetricsSelection) return;
            renderHostMetrics(metrics?.available ? metrics : null, metrics?.available
                ? 'Live metrics · refreshes every 15 seconds' : (metrics?.message || 'Metrics unavailable.'));
        } catch (error) {
            if (selection !== hostMetricsSelection) return;
            renderHostMetrics(null, 'Metrics unavailable. Check SSH connectivity.');
        } finally {
            if (selection === hostMetricsSelection) hostMetricsTimer = setTimeout(refresh, 15000);
        }
    }
    refresh();
}

window.addEventListener('pagehide', () => {
    ++hostMetricsSelection;
    clearTimeout(hostMetricsTimer);
    if (hostMetricsAbort) hostMetricsAbort.abort();
});
