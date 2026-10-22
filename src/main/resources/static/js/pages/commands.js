/**
 * commands.js — Commands Management and catalog module.
 *
 * Responsibilities:
 *  - Load commands list from API
 *  - Populate command dropdown (in dashboard execution panel)
 *  - Render commands catalog table with Last Exit Code & Last Executed At
 *  - Target server selector for preconfigured command execution
 *  - Open, edit, create, and delete command templates
 *
 * Depends on: config.js, api.js, ui.js, state.js, router.js (showPage), servers.js (selectServer)
 */

'use strict';

// Issue #29: Resolve executable command string across legacy and refactored templates [Author: Ravindra Singh Pawar / Antigravity]
function getExecutableCommand(cmd) {
    if (!cmd) return '';
    const param = (cmd.parameters || '').trim();
    const name  = (cmd.name || '').trim();
    if (param && (param.includes(' ') || param.startsWith('/') || param.startsWith('./') || !name || name.includes(' '))) {
        return param;
    }
    return [name, param].filter(Boolean).join(' ');
}

// ── Data Loading ──────────────────────────────────────────────────────────

async function loadCommands() {
    try {
        state.commands = unwrap(await apiRequest(API.commands.list, { silent: true })) || [];
        renderCommandsSelect();
        renderCommandsTable();
    } catch (err) {
        showNotice('Failed to load commands: ' + err.message, true);
    }
}

// ── Rendering ─────────────────────────────────────────────────────────────

function renderCommandsSelect() {
    const select = $('command-select');
    if (!select) return;
    const currentVal = select.value;
    select.innerHTML = '<option value="">Custom command</option>' +
        state.commands.map((c) => `<option value="${c.id}">${escapeHtml(c.name || c.description)}</option>`).join('');
    if (state.commands.some((c) => String(c.id) === currentVal)) {
        select.value = currentVal;
    }
}

// Issue #29: Render commands table with Last Exit Code & Last Executed At columns [Author: Ravindra Singh Pawar / Antigravity]
function renderCommandsTable() {
    const container = $('command-table');
    if (!container) return;

    const rows = state.commands.map((c) => {
        const isCompleted = c.status === 'COMPLETED';
        const isFailed = c.status === 'FAILED';
        let exitStatusBadge = '<span class="status unknown"><span class="pulse-dot"></span>NOT RUN</span>';
        if (isCompleted) {
            exitStatusBadge = '<span class="status up"><span class="pulse-dot"></span>EXIT 0</span>';
        } else if (isFailed) {
            exitStatusBadge = '<span class="status down"><span class="pulse-dot"></span>EXIT 1</span>';
        } else if (c.status) {
            exitStatusBadge = `<span class="status info">${escapeHtml(c.status)}</span>`;
        }

        const lastExecutedAt = c.lastUpdatedDttm ? formatDttm(c.lastUpdatedDttm) : '-';

        return `
            <tr>
                <td><strong>#${c.id}</strong></td>
                <td><strong>${escapeHtml(c.name || 'Unnamed Template')}</strong></td>
                <td><code>${escapeHtml(getExecutableCommand(c))}</code></td>
                <td>${escapeHtml(c.description || '-')}</td>
                <td>${exitStatusBadge}</td>
                <td style="font-size: 12px; color: var(--muted);">${escapeHtml(lastExecutedAt)}</td>
                <td>
                    <div class="service-actions">
                        <button class="btn success btn-sm" data-cmd-run="${c.id}">Run</button>
                        <button class="btn secondary btn-sm" data-cmd-edit="${c.id}">Edit</button>
                        <button class="btn danger btn-sm"    data-cmd-delete="${c.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    });

    renderTable(
        container,
        ['ID', 'Template Name', 'Command String', 'Description', 'Last Exit Code', 'Last Executed At', 'Actions'],
        rows,
        'No configured commands. Click "+ Add command" to create reusable SSH commands.',
        {
            // Issue #29: Run command against target server context and stream to dashboard console [Author: Ravindra Singh Pawar / Antigravity]
            '[data-cmd-run]': async (b) => {
                const cmdId = Number(b.dataset.cmdRun);
                const cmd = state.commands.find((c) => c.id === cmdId);
                if (!cmd) return;

                const targetSelect = $('command-target-server');
                const targetServerId = Number(targetSelect ? targetSelect.value : state.selectedServer?.id);
                if (!targetServerId) {
                    showNotice('Please select a target server from the dropdown to run this command.', true);
                    targetSelect?.focus();
                    return;
                }

                const targetServer = state.servers.find((s) => s.id === targetServerId);
                if (!targetServer) {
                    showNotice('Selected target server not found.', true);
                    return;
                }

                await selectServer(targetServer.id);
                const fullCmd = getExecutableCommand(cmd);
                if ($('command')) $('command').value = fullCmd;

                showPage('dashboard');
                const terminal = $('output');
                if (terminal) {
                    terminal.textContent = `Executing: ${fullCmd} on ${targetServer.hostname || targetServer.ipAddress}...\n`;
                }
                showNotice(`Running "${cmd.name || fullCmd}" on ${targetServer.hostname || targetServer.ipAddress}...`);

                try {
                    const res = await apiRequest(API.commands.execute(targetServer.id, fullCmd, cmd.id), { method: 'POST' });
                    const data = unwrap(res);
                    if (terminal) {
                        terminal.textContent = typeof data === 'object' ? JSON.stringify(data, null, 2) : String(data);
                        terminal.scrollTop = terminal.scrollHeight;
                    }
                    showNotice('Command completed successfully.');
                    await loadCommands();
                } catch (err) {
                    if (terminal) {
                        terminal.textContent += `\n[ERROR]: ${err.message}`;
                        terminal.scrollTop = terminal.scrollHeight;
                    }
                    showNotice(`Command execution failed: ${err.message}`, true);
                    await loadCommands();
                }
            },
            '[data-cmd-edit]':   (b) => openCommandModal(Number(b.dataset.cmdEdit)),
            '[data-cmd-delete]': (b) => deleteCommand(Number(b.dataset.cmdDelete)),
        }
    );
}

// ── Modal & CRUD ──────────────────────────────────────────────────────────

function openCommandModal(id = null) {
    $('command-crud-form').reset();
    $('command-id').value = id || '';
    if (id) {
        const cmd = state.commands.find((c) => c.id === id);
        if (!cmd) return showNotice('Command not found.', true);
        $('command-name').value        = cmd.name        || '';
        $('command-description').value = cmd.description || '';
        $('command-parameters').value  = cmd.parameters  || '';
        $('command-status').value      = cmd.status      || 'CREATED';
        $('command-result').value      = cmd.result      || '';
    } else {
        $('command-status').value = 'CREATED';
    }
    $('command-modal').classList.add('active');
}

async function deleteCommand(id) {
    if (!window.confirm('Are you sure you want to delete this command?')) return;
    try {
        await apiRequest(API.commands.byId(id), { method: 'DELETE' });
        await loadCommands();
        showNotice('Command deleted successfully.');
    } catch (err) {
        showNotice('Failed to delete command: ' + err.message, true);
    }
}

// ── Event Handlers ─────────────────────────────────────────────────────────

function initCommandsPage() {
    // Issue #29: Sync target server selector with active server context [Author: Ravindra Singh Pawar / Antigravity]
    $('command-target-server')?.addEventListener('change', (e) => {
        const val = Number(e.target.value);
        if (val) {
            selectServer(val);
        } else {
            selectServer(null);
        }
    });

    $('command-crud-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id = $('command-id').value;
        const payload = {
            name:              $('command-name').value.trim(),
            description:       $('command-description').value.trim(),
            parameters:        $('command-parameters').value.trim(),
            status:            $('command-status').value,
            result:            $('command-result').value,
            createdSource:     'UI',
            lastUpdatedSource: 'UI',
        };
        try {
            await apiRequest(id ? API.commands.byId(id) : API.commands.list, {
                method: id ? 'PUT' : 'POST',
                body:   JSON.stringify(payload),
            });
            $('command-modal').classList.remove('active');
            await loadCommands();
            showNotice(id ? 'Command updated successfully.' : 'Command created successfully.');
        } catch (err) {
            showNotice('Failed to save command: ' + err.message, true);
        }
    });

    $('add-command')?.addEventListener('click',      () => openCommandModal());
    $('refresh-commands')?.addEventListener('click', loadCommands);
}
