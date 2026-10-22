/**
 * servers.js — Server Inventory page module.
 *
 * Responsibilities:
 *  - Load and cache server list from the API
 *  - Render server inventory table
 *  - Populate server-select dropdowns (shared across pages)
 *  - Open / save / delete server modal
 *  - Select active server context (triggers service load)
 *
 * Depends on: config.js, api.js, ui.js, state.js
 */

'use strict';

// ── Data Loading ──────────────────────────────────────────────────────────

async function loadServers() {
    try {
        state.servers = unwrap(await apiRequest(API.servers.list, { silent: true })) || [];
        renderServerSelectDropdowns();
        renderServersTable();
        updateDashboardStats();

        if (!state.selectedServer && state.servers.length > 0) {
            await selectServer(state.servers[0].id);
        } else if (state.selectedServer) {
            const refreshed = state.servers.find((s) => s.id === state.selectedServer.id);
            if (refreshed) {
                state.selectedServer = refreshed;
                await selectServer(refreshed.id);
            }
        }
    } catch (err) {
        showNotice('Failed to load servers: ' + err.message, true);
    }
}

// ── Rendering ─────────────────────────────────────────────────────────────

function renderServerSelectDropdowns() {
    const optHtml = state.servers
        .map((s) => `<option value="${s.id}">${escapeHtml(s.hostname || s.ipAddress)} (${escapeHtml(s.ipAddress || '')})</option>`)
        .join('');

    const populate = (id, prefix) => {
        const el = $(id);
        if (el) el.innerHTML = `<option value="">${prefix}</option>${optHtml}`;
    };

    populate('server-select',       '-- Select a Server --');
    populate('service-server',      '-- Select Server --');
    populate('service-filter-server', 'All Servers');
    // Issue #29: Populate command target server dropdown [Author: Ravindra Singh Pawar / Antigravity]
    populate('command-target-server', '-- Select Target Server --');
    populate('alert-server',        'Select Server');
    populate('event-log-server',    'Optional Server');
    populate('path-server',         'Select Server');

    if ($('server-select') && state.selectedServer) {
        $('server-select').value = state.selectedServer.id;
    }
    if ($('command-target-server') && state.selectedServer) {
        $('command-target-server').value = state.selectedServer.id;
    }
}

function renderServersTable() {
    const rows = state.servers.map((s) => {
        const statusClass  = (s.status || 'UNKNOWN').toLowerCase();
        const serviceCount = s.services?.length || 0;
        return `
            <tr>
                <td><strong>#${s.id}</strong></td>
                <td><strong>${escapeHtml(s.hostname || 'Unnamed')}</strong></td>
                <td>
                    <code>${escapeHtml(s.ipAddress || '')}</code>
                    <div style="font-size: 11.5px; color: var(--muted); margin-top: 2px;">
                        <span style="font-family: var(--font-mono);">:${s.sshPort || 22}</span> (${escapeHtml(s.sshUsername || 'ubuntu')})
                    </div>
                </td>
                <td>${escapeHtml(s.osType || 'Linux')}</td>
                <td><span class="status ${statusClass}"><span class="pulse-dot"></span>${escapeHtml(s.status || 'UNKNOWN')}</span></td>
                <td>${serviceCount} service(s)</td>
                <td>
                    <div class="service-actions">
                        <button class="btn secondary btn-sm" data-server-select="${s.id}">Dashboard</button>
                        <button class="btn secondary btn-sm" data-server-edit="${s.id}">Edit</button>
                        <button class="btn danger btn-sm"    data-server-delete="${s.id}">Delete</button>
                    </div>
                </td>
            </tr>`;
    });

    renderTable(
        $('server-table'),
        ['ID', 'Hostname', 'IP Address / SSH', 'OS', 'Status', 'Services', 'Actions'],
        rows,
        'No servers registered. Click "+ Add server" to add your first server.',
        {
            '[data-server-select]': (b) => { selectServer(Number(b.dataset.serverSelect)); showPage('dashboard'); },
            '[data-server-edit]':   (b) => openServerModal(Number(b.dataset.serverEdit)),
            '[data-server-delete]': (b) => deleteServer(Number(b.dataset.serverDelete)),
        }
    );
}

// ── Server Selection ──────────────────────────────────────────────────────

async function selectServer(id) {
    if (!id) {
        state.selectedServer = null;
        if ($('server-select')) $('server-select').value = '';
        if ($('command-target-server')) $('command-target-server').value = '';
        if ($('selected-title')) $('selected-title').textContent = 'Select a server';
        if ($('selected-meta')) $('selected-meta').textContent  = 'Choose a server to view and control its services.';
        if ($('service-list'))  $('service-list').innerHTML     = '<div class="empty-state">Select a server to load services.</div>';
        return;
    }

    const server = state.servers.find((s) => s.id === id);
    if (!server) return;

    state.selectedServer = server;
    if ($('server-select')) $('server-select').value = server.id;
    // Issue #29: Sync command target server dropdown with selected server [Author: Ravindra Singh Pawar / Antigravity]
    if ($('command-target-server')) $('command-target-server').value = server.id;

    if ($('selected-title')) {
        $('selected-title').textContent = server.hostname || server.ipAddress;
    }
    if ($('selected-meta')) {
        $('selected-meta').innerHTML =
            `IP: <strong>${escapeHtml(server.ipAddress || 'N/A')}</strong> · ` +
            `OS: <strong>${escapeHtml(server.osType || 'Linux')}</strong> · ` +
            `Status: <span class="status ${(server.status || '').toLowerCase()}">` +
            `<span class="pulse-dot"></span>${escapeHtml(server.status || 'UNKNOWN')}</span>`;
    }

    try {
        const persisted = unwrap(await apiRequest(API.services.healthStatus(id), { silent: true })) || [];
        state.healthStatuses = Object.fromEntries(persisted.map((item) => [item.service?.id, item.status]));
    } catch {
        state.healthStatuses = {};
    }

    try {
        state.serverServiceConfigurations =
            unwrap(await apiRequest(API.services.serverServiceConfigs(id), { silent: true })) || [];
    } catch {
        state.serverServiceConfigurations = [];
    }

    await loadServices();
    updateDashboardStats();
}

// ── Modal ─────────────────────────────────────────────────────────────────

// Issue #26: Connection feedback helper [Author: Ravindra Singh Pawar / Antigravity]
function showConnectionFeedback(status, message) {
    const box = $('connection-test-feedback');
    if (!box) return;
    box.style.display = 'block';
    if (status === true) {
        box.style.background = 'var(--ok-light)';
        box.style.color = '#065f46';
        box.style.border = '1px solid var(--ok-border)';
    } else if (status === false) {
        box.style.background = 'var(--bad-light)';
        box.style.color = '#991b1b';
        box.style.border = '1px solid var(--bad-border)';
    } else {
        box.style.background = 'var(--paper)';
        box.style.color = 'var(--muted)';
        box.style.border = '1px solid var(--line)';
    }
    box.textContent = message;
}

function openServerModal(id = null) {
    $('server-form').reset();
    $('server-id').value = id || '';
    $('os-type').value   = 'Linux';
    $('ssh-port').value  = '22';
    $('ssh-username').value = 'ubuntu';

    const feedback = $('connection-test-feedback');
    if (feedback) {
        feedback.style.display = 'none';
        feedback.textContent = '';
    }

    if (id) {
        const server = state.servers.find((s) => s.id === id);
        if (!server) return showNotice('Server not found.', true);
        $('hostname').value     = server.hostname  || '';
        $('ip-address').value   = server.ipAddress || '';
        $('os-type').value      = server.osType    || 'Linux';
        $('ssh-port').value     = server.sshPort   || 22;
        $('ssh-username').value = server.sshUsername || 'ubuntu';
    }
    $('server-modal').classList.add('active');
}

async function deleteServer(id) {
    if (!window.confirm('Are you sure you want to delete this server?')) return;
    try {
        await apiRequest(API.servers.byId(id), { method: 'DELETE' });
        if (state.selectedServer?.id === id) state.selectedServer = null;
        await loadServers();
        showNotice('Server deleted successfully.');
    } catch (err) {
        showNotice('Failed to delete server: ' + err.message, true);
    }
}

// ── Form Handlers ─────────────────────────────────────────────────────────

function initServerFormHandlers() {
    // Issue #26: Handle Server Save with SSH fields & dynamic status [Author: Ravindra Singh Pawar / Antigravity]
    $('server-form').onsubmit = async (e) => {
        e.preventDefault();
        const id = $('server-id').value;
        const existingServer = id ? state.servers.find((s) => s.id === Number(id)) : null;
        const payload = {
            id:                id ? Number(id) : null,
            hostname:          $('hostname').value.trim(),
            ipAddress:         $('ip-address').value.trim(),
            osType:            $('os-type').value.trim(),
            status:            existingServer ? existingServer.status : 'UNKNOWN',
            sshPort:           Number($('ssh-port').value) || 22,
            sshUsername:       $('ssh-username').value.trim() || 'ubuntu',
            paths:             [],
            services:          [],
            createdSource:     'UI',
            lastUpdatedSource: 'UI',
        };
        try {
            await apiRequest(id ? API.servers.byId(id) : API.servers.list, {
                method: id ? 'PUT' : 'POST',
                body:   JSON.stringify(payload),
            });
            $('server-modal').classList.remove('active');
            await loadServers();
            showNotice(id ? 'Server updated successfully.' : 'Server created successfully.');
        } catch (err) {
            showNotice('Failed to save server: ' + err.message, true);
        }
    };

    // Issue #26: Test SSH connection handler [Author: Ravindra Singh Pawar / Antigravity]
    const testBtn = $('btn-test-connection');
    if (testBtn) {
        testBtn.onclick = async () => {
            const ipAddress = $('ip-address').value.trim();
            if (!ipAddress) {
                showConnectionFeedback(false, 'Please enter an IP address or DNS before testing.');
                return;
            }

            const testPayload = {
                id:          $('server-id').value ? Number($('server-id').value) : null,
                hostname:    $('hostname').value.trim() || ipAddress,
                ipAddress:   ipAddress,
                osType:      $('os-type').value.trim() || 'Linux',
                sshPort:     Number($('ssh-port').value) || 22,
                sshUsername: $('ssh-username').value.trim() || 'ubuntu',
            };

            testBtn.disabled = true;
            testBtn.textContent = '⏳ Testing SSH...';
            showConnectionFeedback(null, 'Attempting SSH handshake via Vault Keyless CA...');

            try {
                const res = await apiRequest(API.servers.testConnection, {
                    method: 'POST',
                    body:   JSON.stringify(testPayload),
                    silent: true,
                });
                const data = res?.data || res;
                if (data && data.connected) {
                    showConnectionFeedback(true, `✅ ${data.message || 'Connection successful!'}`);
                } else {
                    showConnectionFeedback(false, `❌ ${data?.message || 'Connection refused or unreachable.'}`);
                }
            } catch (err) {
                showConnectionFeedback(false, `❌ ${err.message || 'Connection test failed.'}`);
            } finally {
                testBtn.disabled = false;
                testBtn.textContent = '🔌 Test Connection';
            }
        };
    }

    $('server-select').onchange = (e) => selectServer(Number(e.target.value));
    $('add-server')?.addEventListener('click',      () => openServerModal());
    $('add-server-page')?.addEventListener('click', () => openServerModal());
}
