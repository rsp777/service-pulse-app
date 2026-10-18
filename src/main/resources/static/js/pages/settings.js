/**
 * settings.js — Settings & Runtime Configuration module.
 *
 * Responsibilities:
 *  - Manage runtime configuration feature flags & cron schedules
 *  - Paths catalog (SCRIPTS_HOME, LOGS_HOME, custom paths) CRUD
 *  - Execution Scripts catalog (.sh, .py, etc.) CRUD
 *
 * Depends on: config.js, api.js, ui.js, state.js, servers.js (renderServerSelectDropdowns)
 */

'use strict';

// ── Data Loading ──────────────────────────────────────────────────────────

async function loadSettings() {
    try {
        state.paths   = unwrap(await apiRequest(API.paths.list, { silent: true })) || [];
        state.scripts = unwrap(await apiRequest(API.scripts.list, { silent: true })) || [];
        const runtime = unwrap(await apiRequest(API.config.runtime, { silent: true }));

        if ($('healthcheck-enabled'))               $('healthcheck-enabled').checked               = Boolean(runtime?.healthCheckEnabled);
        if ($('service-management-enabled'))        $('service-management-enabled').checked        = Boolean(runtime?.serviceManagementEnabled);
        if ($('alert-management-enabled'))          $('alert-management-enabled').checked          = Boolean(runtime?.alertManagementEnabled);
        if ($('health-status-population-enabled')) $('health-status-population-enabled').checked = Boolean(runtime?.healthStatusPopulationEnabled);
        if ($('healthcheck-cron'))                  $('healthcheck-cron').value                   = runtime?.healthCheckCron || '';
        if ($('runtime-config-status'))             $('runtime-config-status').textContent        = 'Configuration Loaded';

        renderPaths();
        renderScripts();
    } catch (err) {
        if ($('runtime-config-status')) $('runtime-config-status').textContent = 'Unavailable';
        showNotice('Failed to load settings: ' + err.message, true);
    }
}

// ── Paths Catalog ─────────────────────────────────────────────────────────

function renderPaths() {
    const container = $('path-table');
    if (!container) return;

    const rows = state.paths.map((p) => `
        <tr>
            <td><strong>#${p.id}</strong></td>
            <td><strong>${escapeHtml(p.pathName)}</strong></td>
            <td><code>${escapeHtml(p.pathDescription || '')}</code></td>
            <td>
                <div class="service-actions">
                    <button class="btn secondary btn-sm" data-path-edit="${p.id}">Edit</button>
                    <button class="btn danger btn-sm"    data-path-delete="${p.id}">Delete</button>
                </div>
            </td>
        </tr>
    `);

    renderTable(
        container,
        ['ID', 'Path Name', 'Location / Description', 'Actions'],
        rows,
        'No paths configured. Click "+ Add path" to configure directory locations.',
        {
            '[data-path-edit]':   (b) => openPathModal(Number(b.dataset.pathEdit)),
            '[data-path-delete]': (b) => deletePath(Number(b.dataset.pathDelete)),
        }
    );
}

function openPathModal(id = null) {
    $('path-crud-form').reset();
    $('path-id').value = id || '';
    renderServerSelectDropdowns();

    if (id) {
        const p = state.paths.find((item) => item.id === id);
        if (!p) return showNotice('Path not found.', true);
        $('path-name').value        = p.pathName || '';
        $('path-description').value = p.pathDescription || '';
        if (p.servers && p.servers.length > 0) {
            $('path-server').value = p.servers[0].id;
        }
    } else if (state.selectedServer) {
        $('path-server').value = state.selectedServer.id;
    }
    $('path-modal').classList.add('active');
}

async function deletePath(id) {
    if (!window.confirm('Delete this path?')) return;
    try {
        await apiRequest(API.paths.byId(id), { method: 'DELETE' });
        await loadSettings();
        showNotice('Path deleted.');
    } catch (err) {
        showNotice('Failed to delete path: ' + err.message, true);
    }
}

// ── Scripts Catalog ───────────────────────────────────────────────────────

function renderScripts() {
    const container = $('script-table');
    if (!container) return;

    const rows = state.scripts.map((s) => `
        <tr>
            <td><strong>#${s.id}</strong></td>
            <td><strong>${escapeHtml(s.scriptName)}</strong></td>
            <td><span class="status info">${escapeHtml(s.scriptExtension || 'SH')}</span></td>
            <td>
                <div class="service-actions">
                    <button class="btn secondary btn-sm" data-script-edit="${s.id}">Edit</button>
                    <button class="btn danger btn-sm"    data-script-delete="${s.id}">Delete</button>
                </div>
            </td>
        </tr>
    `);

    renderTable(
        container,
        ['ID', 'Script Name', 'Extension', 'Actions'],
        rows,
        'No scripts configured. Click "+ Add script" to register lifecycle scripts.',
        {
            '[data-script-edit]':   (b) => openScriptModal(Number(b.dataset.scriptEdit)),
            '[data-script-delete]': (b) => deleteScript(Number(b.dataset.scriptDelete)),
        }
    );
}

function openScriptModal(id = null) {
    $('script-crud-form').reset();
    $('script-id').value = id || '';
    if (id) {
        const s = state.scripts.find((item) => item.id === id);
        if (!s) return showNotice('Script not found.', true);
        $('script-name').value      = s.scriptName || '';
        $('script-extension').value = s.scriptExtension || 'SH';
    }
    $('script-modal').classList.add('active');
}

async function deleteScript(id) {
    if (!window.confirm('Delete this script?')) return;
    try {
        await apiRequest(API.scripts.byId(id), { method: 'DELETE' });
        await loadSettings();
        showNotice('Script deleted.');
    } catch (err) {
        showNotice('Failed to delete script: ' + err.message, true);
    }
}

// ── Event Handlers ─────────────────────────────────────────────────────────

function initSettingsPage() {
    // Runtime Config save
    $('save-runtime-config')?.addEventListener('click', async () => {
        try {
            const runtime = unwrap(await apiRequest(API.config.runtime, {
                method: 'PUT',
                body: JSON.stringify({
                    healthCheckEnabled:            $('healthcheck-enabled').checked,
                    serviceManagementEnabled:     $('service-management-enabled').checked,
                    alertManagementEnabled:       $('alert-management-enabled').checked,
                    healthStatusPopulationEnabled: $('health-status-population-enabled').checked,
                }),
            }));
            $('healthcheck-enabled').checked               = Boolean(runtime?.healthCheckEnabled);
            $('service-management-enabled').checked        = Boolean(runtime?.serviceManagementEnabled);
            $('alert-management-enabled').checked          = Boolean(runtime?.alertManagementEnabled);
            $('health-status-population-enabled').checked = Boolean(runtime?.healthStatusPopulationEnabled);
            if ($('runtime-config-status')) $('runtime-config-status').textContent = 'Saved';
            showNotice('Runtime configuration saved successfully.');
        } catch (err) {
            showNotice('Failed to save runtime configuration: ' + err.message, true);
        }
    });

    // Path Form
    $('path-crud-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const serverId = Number($('path-server').value);
        const server   = state.servers.find((s) => s.id === serverId);
        if (!server) return showNotice('Please select a server.', true);

        const id = $('path-id').value;
        const payload = {
            id:                id ? Number(id) : null,
            pathName:          $('path-name').value.trim(),
            pathDescription:   $('path-description').value.trim(),
            servers:           [server],
            scripts:           [],
            createdSource:     'UI',
            lastUpdatedSource: 'UI',
        };

        try {
            await apiRequest(id ? API.paths.byId(id) : API.paths.list, {
                method: id ? 'PUT' : 'POST',
                body:   JSON.stringify(payload),
            });
            $('path-modal').classList.remove('active');
            await loadSettings();
            showNotice('Path saved successfully.');
        } catch (err) {
            showNotice('Failed to save path: ' + err.message, true);
        }
    });

    // Script Form
    $('script-crud-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = $('script-id').value;
        const payload = {
            id:                id ? Number(id) : null,
            scriptName:        $('script-name').value.trim(),
            scriptExtension:   $('script-extension').value.trim(),
            paths:             [],
            createdSource:     'UI',
            lastUpdatedSource: 'UI',
        };

        try {
            await apiRequest(id ? API.scripts.byId(id) : API.scripts.list, {
                method: id ? 'PUT' : 'POST',
                body:   JSON.stringify(payload),
            });
            $('script-modal').classList.remove('active');
            await loadSettings();
            showNotice('Script saved successfully.');
        } catch (err) {
            showNotice('Failed to save script: ' + err.message, true);
        }
    });

    $('add-path')?.addEventListener('click',   () => openPathModal());
    $('add-script')?.addEventListener('click', () => openScriptModal());
}
