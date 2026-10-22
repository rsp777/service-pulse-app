/**
 * dashboard.js — Dashboard stats + service panel module.
 *
 * Responsibilities:
 *  - Update KPI stat cards
 *  - Render the monitored-services list for the selected server
 *  - Service lifecycle actions (start / stop / restart / logs)
 *  - Bulk service actions with SSE streaming output
 *  - Single-service log streaming
 *  - SSH command execution panel
 *
 * Depends on: config.js, api.js, ui.js, state.js, servers.js (selectServer)
 */

'use strict';

// ── KPI Stats ─────────────────────────────────────────────────────────────

function updateDashboardStats() {
    let totalServices = 0;
    let onlineServices = 0;

    state.servers.forEach((s) => {
        const services = s.services || [];
        totalServices += services.length;
        services.forEach((svc) => {
            if (state.healthStatuses[svc.id] === 'UP') onlineServices++;
        });
    });

    const setIfPresent = (id, val) => { if ($(id)) $(id).textContent = val; };
    setIfPresent('stat-servers-count',  state.servers.length);
    setIfPresent('stat-services-count', `${onlineServices} / ${totalServices}`);
    setIfPresent('stat-alerts-count',   state.unreadAlerts.length);
    setIfPresent('stat-ssh-status',     state.servers.length > 0 ? 'Active' : 'Idle');
}

// ── Service List ──────────────────────────────────────────────────────────

function getServiceHealthUrl(service) {
    return state.serverServiceConfigurations.find((c) => c.serviceId === service.id)?.healthCheckUrl || '';
}

async function loadServices() {
    if (!state.selectedServer) return;
    try {
        const persisted = unwrap(await apiRequest(API.services.healthStatus(state.selectedServer.id), { silent: true })) || [];
        state.healthStatuses = Object.fromEntries(persisted.map((item) => [item.service?.id, item.status]));
    } catch {
        // preserve existing state.healthStatuses
    }
    const services    = state.selectedServer.services || [];
    const hasMultiple = services.length > 1;

    ['start-all-services', 'stop-all-services', 'restart-all-services'].forEach((id) => {
        const btn = $(id);
        if (btn) btn.hidden = !hasMultiple;
    });

    const container = $('service-list');
    if (!container) return;

    if (!services.length) {
        container.innerHTML = '<div class="empty-state">No services are associated with this server. Click "+ Add service" above to configure one.</div>';
        return;
    }

    container.innerHTML = services.map((service) => {
        const healthUrl  = getServiceHealthUrl(service);
        const statusVal  = state.healthStatuses[service.id] || 'UNKNOWN';
        const statusClass = statusVal === 'UP' ? 'online' : statusVal === 'DOWN' ? 'offline' : 'unknown';
        return `
            <div class="service-row" data-service-id="${service.id}">
                <div class="service-info">
                    <strong>${escapeHtml(service.serviceName)}</strong>
                    <small>Health URL: ${escapeHtml(healthUrl || 'Not configured')}</small>
                    <small>Version: <span id="version-${service.id}">Unknown</span>
                           &nbsp;·&nbsp;Status:
                           <span id="indicator-${service.id}" class="status ${statusClass}">
                               <span class="pulse-dot"></span>${escapeHtml(statusVal)}
                           </span>
                    </small>
                </div>
                <div class="service-actions">
                    <button class="btn success btn-sm"   data-service-action="start">Start</button>
                    <button class="btn secondary btn-sm" data-service-action="stop">Stop</button>
                    <button class="btn secondary btn-sm" data-service-action="restart">Restart</button>
                    <button class="btn secondary btn-sm" data-service-action="logs">Logs</button>
                    <button class="btn secondary btn-sm" data-service-edit="${service.id}">Edit</button>
                    <button class="btn danger btn-sm"    data-service-delete="${service.id}">Delete</button>
                </div>
            </div>`;
    }).join('');

    container.querySelectorAll('[data-service-action]').forEach((btn) => {
        btn.onclick = () => {
            const action    = btn.dataset.serviceAction;
            const serviceId = Number(btn.closest('[data-service-id]').dataset.serviceId);
            serviceAction(action, serviceId);
        };
    });
    container.querySelectorAll('[data-service-edit]').forEach((btn) => {
        btn.onclick = () => openServiceModal(Number(btn.dataset.serviceEdit));
    });
    container.querySelectorAll('[data-service-delete]').forEach((btn) => {
        btn.onclick = () => deleteService(Number(btn.dataset.serviceDelete));
    });

    services.forEach((s) => updateServiceRuntime(s));
}

// Issue #27: Proxied runtime status endpoint for robust Actuator parsing [Author: Ravindra Singh Pawar / Antigravity]
async function updateServiceRuntime(service) {
    const healthUrl = getServiceHealthUrl(service);
    const versionEl   = $(`version-${service.id}`);
    const indicatorEl = $(`indicator-${service.id}`);

    // Issue #30: If health check URL is unconfigured, synchronize indicator with persisted host health status [Author: Ravindra Singh Pawar / Antigravity]
    if (!healthUrl) {
        if (versionEl) versionEl.textContent = '—';
        const persisted = state.healthStatuses[service.id];
        if (indicatorEl && persisted) {
            const isOnline = persisted === 'ONLINE' || persisted === 'UP';
            indicatorEl.className = `status ${isOnline ? 'online' : 'offline'}`;
            indicatorEl.innerHTML = `<span class="pulse-dot"></span>${escapeHtml(persisted)}`;
        }
        return;
    }

    try {
        const res = await apiRequest(`${API.services.runtimeStatus}?url=${encodeURIComponent(healthUrl)}`, { silent: true });
        if (res && res.status) {
            if (indicatorEl) {
                const isOnline = res.status === 'ONLINE' || res.status === 'UP';
                indicatorEl.className = `status ${isOnline ? 'online' : 'offline'}`;
                indicatorEl.innerHTML = `<span class="pulse-dot"></span>${isOnline ? 'ONLINE' : 'OFFLINE'}`;
            }
            if (versionEl) {
                versionEl.textContent = res.version || 'Unknown';
            }
        }
    } catch {
        const persisted = state.healthStatuses[service.id];
        if (indicatorEl && persisted) {
            const isOnline = persisted === 'ONLINE' || persisted === 'UP';
            indicatorEl.className = `status ${isOnline ? 'online' : 'offline'}`;
            indicatorEl.innerHTML = `<span class="pulse-dot"></span>${escapeHtml(persisted)}`;
        }
    }
}

// ── Service Actions ────────────────────────────────────────────────────────

async function serviceAction(action, serviceId) {
    if (action === 'logs') {
        streamServiceLogs(serviceId);
        return;
    }

    showNotice(`Executing ${action} on service #${serviceId}...`);
    try {
        const res        = await apiRequest(API.services.execute(action, state.selectedServer.id, serviceId), { method: 'POST' });
        const data       = unwrap(res);
        const outputText = typeof data === 'object'
            ? (data.output || data.payload?.output || JSON.stringify(data, null, 2))
            : String(data);

        const terminal = $('output');
        if (terminal) terminal.textContent = outputText.replace(/\\n/g, '\n').replace(/\\r/g, '');

        showNotice(`${action} completed successfully.`);
        await selectServer(state.selectedServer.id);
    } catch (err) {
        showNotice(`Service ${action} failed: ${err.message}`, true);
    }
}

function streamServiceLogs(serviceId) {
    if (logStreamSource) { logStreamSource.close(); logStreamSource = null; }

    const terminal = $('logs-output');
    terminal.textContent = 'Connecting to log stream...\n';
    $('logs-modal').classList.add('active');

    logStreamSource = new EventSource(apiUrl(API.services.logStream(state.selectedServer.id, serviceId)));

    logStreamSource.addEventListener('log', (event) => {
        terminal.textContent += `${event.data}\n`;
        terminal.scrollTop    = terminal.scrollHeight;
    });
    logStreamSource.onerror = () => {
        if (logStreamSource) { logStreamSource.close(); logStreamSource = null; }
        terminal.textContent += '\n[Stream disconnected]\n';
    };
}

async function bulkServiceAction(action) {
    if (!state.selectedServer) return showNotice('Select a server first.', true);
    const serverId = state.selectedServer.id;

    if (serviceActionStreamSource) { serviceActionStreamSource.close(); serviceActionStreamSource = null; }

    const terminal = $('output');
    terminal.textContent = `Running ${action} all services on server #${serverId}...\n`;

    serviceActionStreamSource = new EventSource(apiUrl(API.services.bulkStream(action, serverId)));

    serviceActionStreamSource.addEventListener('output', (event) => {
        terminal.textContent += `${event.data}\n`;
        terminal.scrollTop    = terminal.scrollHeight;
    });
    serviceActionStreamSource.addEventListener('complete', async () => {
        if (serviceActionStreamSource) { serviceActionStreamSource.close(); serviceActionStreamSource = null; }
        terminal.textContent += `\n[${action.toUpperCase()} ALL COMPLETED]\n`;
        await selectServer(serverId);
        showNotice(`${action} all services completed.`);
    });
    serviceActionStreamSource.onerror = () => {
        if (serviceActionStreamSource) { serviceActionStreamSource.close(); serviceActionStreamSource = null; }
        terminal.textContent += '\n[Lifecycle stream closed or completed]\n';
    };
}

// ── SSH Command Execution ──────────────────────────────────────────────────

function initCommandExecutionPanel() {
    // Issue #29: Resolve executable command string for command-select dropdown [Author: Ravindra Singh Pawar / Antigravity]
    $('command-select').onchange = (e) => {
        const cmd = state.commands.find((c) => String(c.id) === e.target.value);
        if (cmd) $('command').value = typeof getExecutableCommand === 'function' ? getExecutableCommand(cmd) : [cmd.name, cmd.parameters].filter(Boolean).join(' ');
    };

    $('command-form').onsubmit = async (e) => {
        e.preventDefault();
        if (!state.selectedServer) return showNotice('Select a server first.', true);
        const cmdText = $('command').value.trim();
        if (!cmdText)  return showNotice('Please enter a command to execute.', true);

        const terminal = $('output');
        terminal.textContent = `Executing: ${cmdText} on ${state.selectedServer.hostname || state.selectedServer.ipAddress}...\n`;

        try {
            const res  = await apiRequest(API.commands.execute(state.selectedServer.id, cmdText), { method: 'POST' });
            const data = unwrap(res);
            terminal.textContent = typeof data === 'object' ? JSON.stringify(data, null, 2) : String(data);
            terminal.scrollTop   = terminal.scrollHeight;
            showNotice('Command completed.');
        } catch (err) {
            terminal.textContent += `\n[ERROR]: ${err.message}`;
            showNotice('Command failed: ' + err.message, true);
        }
    };
}

// ── Terminal Controls ──────────────────────────────────────────────────────

function initTerminalControls() {
    $('terminal-copy-btn')?.addEventListener('click', () => {
        const text = $('output')?.textContent;
        if (text) {
            navigator.clipboard.writeText(text);
            showNotice('Terminal output copied to clipboard.');
        }
    });

    $('terminal-clear-btn')?.addEventListener('click', () => {
        if ($('output')) $('output').textContent = 'Terminal cleared.';
    });
}

// ── Bulk Action Buttons ────────────────────────────────────────────────────

function initBulkServiceButtons() {
    $('refresh-services')?.addEventListener('click',      loadServices);
    $('start-all-services')?.addEventListener('click',   () => bulkServiceAction('start'));
    $('stop-all-services')?.addEventListener('click',    () => bulkServiceAction('stop'));
    $('restart-all-services')?.addEventListener('click', () => bulkServiceAction('restart'));
}
