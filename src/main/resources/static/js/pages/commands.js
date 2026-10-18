/**
 * commands.js — Commands Management and catalog module.
 *
 * Responsibilities:
 *  - Load commands list from API
 *  - Populate command dropdown (in dashboard execution panel)
 *  - Render commands catalog table
 *  - Open, edit, create, and delete command templates
 *
 * Depends on: config.js, api.js, ui.js, state.js, router.js (showPage)
 */

'use strict';

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
        state.commands.map((c) => `<option value="${c.id}">${escapeHtml(c.description || c.name)}</option>`).join('');
    if (state.commands.some((c) => String(c.id) === currentVal)) {
        select.value = currentVal;
    }
}

function renderCommandsTable() {
    const container = $('command-table');
    if (!container) return;

    const rows = state.commands.map((c) => `
        <tr>
            <td><strong>#${c.id}</strong></td>
            <td><strong>${escapeHtml(c.name)}</strong></td>
            <td><code>${escapeHtml(c.parameters || '-')}</code></td>
            <td>${escapeHtml(c.description || '')}</td>
            <td><span class="status info">${escapeHtml(c.status || 'READY')}</span></td>
            <td>
                <div class="service-actions">
                    <button class="btn success btn-sm" data-cmd-run="${c.id}">Run</button>
                    <button class="btn secondary btn-sm" data-cmd-edit="${c.id}">Edit</button>
                    <button class="btn danger btn-sm"    data-cmd-delete="${c.id}">Delete</button>
                </div>
            </td>
        </tr>
    `);

    renderTable(
        container,
        ['ID', 'Command Name', 'Parameters / Arguments', 'Description', 'Status', 'Actions'],
        rows,
        'No configured commands. Click "+ Add command" to create reusable SSH commands.',
        {
            '[data-cmd-run]': (b) => {
                const cmd = state.commands.find((c) => c.id === Number(b.dataset.cmdRun));
                if (cmd) {
                    const fullCmd = [cmd.name, cmd.parameters].filter(Boolean).join(' ');
                    $('command').value = fullCmd;
                    showPage('dashboard');
                    $('command-form')?.requestSubmit();
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
