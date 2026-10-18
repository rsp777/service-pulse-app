/**
 * alerts.js — Alert Management, Events history, and Condition Mappings module.
 *
 * Responsibilities:
 *  - Load alert rules, unread events, and historical trigger events
 *  - Update notification bell badge and drop-down list
 *  - Render Alert Rules table, with toggle / edit / delete
 *  - Render Triggered Events history
 *  - Manage Condition Types and Key/Value Mappings
 *  - Handle Alert Rule modal, Log Event modal, and Mapping modal
 *
 * Depends on: config.js, api.js, ui.js, state.js
 */

'use strict';

// ── Data Loading ──────────────────────────────────────────────────────────

async function loadAlertsData() {
    try {
        state.alerts       = await apiRequest(API.alerts.list, { silent: true }) || [];
        state.alertEvents  = await apiRequest(API.alerts.events.list, { silent: true }) || [];
        state.unreadAlerts = await apiRequest(API.alerts.events.unread, { silent: true }) || [];
        updateNotificationBell();

        if (state.activeAlertTab === 'rules') renderAlertRules();
        else if (state.activeAlertTab === 'events') renderAlertEvents();
        else if (state.activeAlertTab === 'conditions') loadConditionTypes();
    } catch (err) {
        showNotice('Failed to load alert data: ' + err.message, true);
    }
}

// ── Notification Bell ─────────────────────────────────────────────────────

function updateNotificationBell() {
    const badge = $('notification-badge');
    const count = state.unreadAlerts.length;
    if (badge) {
        badge.textContent = count;
        badge.style.display = count > 0 ? 'block' : 'none';
    }

    const listContainer = $('notification-list');
    if (!listContainer) return;
    if (!state.unreadAlerts.length) {
        listContainer.innerHTML = '<div class="empty-state" style="padding: 20px;">No unread alerts. All systems operational.</div>';
        return;
    }

    listContainer.innerHTML = state.unreadAlerts.map((e) => `
        <div class="notification-item unread" data-event-id="${e.id}">
            <p><strong>${escapeHtml(e.message || 'Alert Triggered')}</strong></p>
            <span class="time">${escapeHtml(e.triggeredDttm || '')} · Status: ${escapeHtml(e.status || 'TRIGGERED')}</span>
            <button class="mark-read-btn" data-mark-id="${e.id}">Mark as read</button>
        </div>
    `).join('');

    listContainer.querySelectorAll('[data-mark-id]').forEach((b) => {
        b.onclick = async (ev) => {
            ev.stopPropagation();
            const id = Number(b.dataset.markId);
            try {
                await apiRequest(API.alerts.events.markRead(id), { method: 'PUT', silent: true });
                await loadAlertsData();
            } catch (err) {
                showNotice(err.message, true);
            }
        };
    });
}

// ── Alert Rules ───────────────────────────────────────────────────────────

function renderAlertRules() {
    const container = $('alert-rules-table');
    if (!container) return;

    const rows = state.alerts.map((a) => {
        let targetName = 'N/A';
        if (a.targetType === 'SERVER') {
            targetName = state.servers.find((s) => s.id === a.serverId)?.hostname || `Server #${a.serverId}`;
        } else {
            targetName = state.servers.flatMap((s) => s.services || []).find((svc) => svc.id === a.serviceId)?.serviceName || `Service #${a.serviceId}`;
        }

        return `
            <tr>
                <td><strong>#${a.id}</strong></td>
                <td><strong>${escapeHtml(a.name)}</strong></td>
                <td>${escapeHtml(a.targetType)}: <strong>${escapeHtml(targetName)}</strong></td>
                <td><code>${escapeHtml(a.conditionType)} ${escapeHtml(a.operator || '')} ${escapeHtml(a.conditionValue || '')}</code></td>
                <td>
                    <button class="btn btn-sm ${a.enabled ? 'success' : 'secondary'}" data-alert-toggle="${a.id}">
                        ${a.enabled ? 'Enabled' : 'Disabled'}
                    </button>
                </td>
                <td>
                    <div class="service-actions">
                        <button class="btn secondary btn-sm" data-alert-edit="${a.id}">Edit</button>
                        <button class="btn danger btn-sm"    data-alert-delete="${a.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    });

    renderTable(
        container,
        ['ID', 'Rule Name', 'Target', 'Condition', 'State', 'Actions'],
        rows,
        'No alert rules configured. Click "+ Add alert rule" to set up health rules.',
        {
            '[data-alert-toggle]': async (b) => {
                const id = Number(b.dataset.alertToggle);
                try {
                    await apiRequest(API.alerts.toggle(id), { method: 'PUT' });
                    await loadAlertsData();
                } catch (err) {
                    showNotice(err.message, true);
                }
            },
            '[data-alert-edit]':   (b) => openAlertModal(Number(b.dataset.alertEdit)),
            '[data-alert-delete]': (b) => deleteAlertRule(Number(b.dataset.alertDelete)),
        }
    );
}

// ── Triggered Events ──────────────────────────────────────────────────────

function renderAlertEvents() {
    const container = $('alert-events-table');
    if (!container) return;

    const rows = state.alertEvents.map((e) => {
        const statusClass = (e.status || 'TRIGGERED').toLowerCase();
        return `
            <tr>
                <td><strong>#${e.id}</strong></td>
                <td>Alert #${e.alertId || '-'}</td>
                <td>Server #${e.serverId ?? '-'} / Service #${e.serviceId ?? '-'}</td>
                <td><strong>${escapeHtml(e.message || '-')}</strong></td>
                <td><span class="status ${statusClass}"><span class="pulse-dot"></span>${escapeHtml(e.status || 'TRIGGERED')}</span></td>
                <td><small>${escapeHtml(e.triggeredDttm || '-')}</small></td>
            </tr>
        `;
    });

    renderTable(
        container,
        ['ID', 'Alert ID', 'Target Scope', 'Message', 'Status', 'Timestamp'],
        rows,
        'No triggered alert events recorded.'
    );
}

// ── Condition Types & Mappings ────────────────────────────────────────────

async function loadConditionTypes() {
    try {
        state.conditionTypes = await apiRequest(API.alerts.conditionTypes.list, { silent: true }) || [];
        renderConditionTypes();
    } catch (err) {
        showNotice('Failed to load condition types: ' + err.message, true);
    }
}

function renderConditionTypes() {
    const container = $('condition-types-table');
    if (!container) return;

    const rows = state.conditionTypes.map((t) => `
        <tr>
            <td><strong>#${t.id}</strong></td>
            <td><strong>${escapeHtml(t.typeName)}</strong></td>
            <td>${escapeHtml(t.description || '')}</td>
            <td>
                <button class="btn secondary btn-sm" data-view-mappings="${t.id}" data-type-name="${escapeHtml(t.typeName)}">
                    View Mappings
                </button>
            </td>
        </tr>
    `);

    renderTable(
        container,
        ['ID', 'Type Name', 'Description', 'Mappings'],
        rows,
        'No condition types available.',
        {
            '[data-view-mappings]': (b) => {
                const typeId   = Number(b.dataset.viewMappings);
                const typeName = b.dataset.typeName;
                loadConditionMappings(typeId, typeName);
            },
        }
    );
}

async function loadConditionMappings(typeId, typeName) {
    state.selectedConditionType = { id: typeId, name: typeName };
    try {
        state.conditionMappings = await apiRequest(API.alerts.conditionTypes.mappings(typeId), { silent: true }) || [];
        renderConditionMappings();
    } catch (err) {
        showNotice('Failed to load condition mappings: ' + err.message, true);
    }
}

function renderConditionMappings() {
    const container = $('condition-mappings-container');
    if (!container) return;

    container.style.display = 'block';
    const titleEl = $('selected-condition-type-title');
    if (titleEl) titleEl.textContent = `Mappings for: ${state.selectedConditionType.name}`;

    const listContainer = $('condition-mappings-list');
    const rows = state.conditionMappings.map((m) => `
        <tr>
            <td><strong>${escapeHtml(m.conditionKey)}</strong></td>
            <td><code>${escapeHtml(m.conditionValue)}</code></td>
            <td>${escapeHtml(m.description || '')}</td>
            <td>
                <button class="btn danger btn-sm" data-delete-mapping="${m.id}">Delete</button>
            </td>
        </tr>
    `);

    renderTable(
        listContainer,
        ['Key', 'Value', 'Description', 'Action'],
        rows,
        'No mappings for this type. Click "+ Add mapping" to add one.',
        {
            '[data-delete-mapping]': async (b) => {
                if (!window.confirm('Delete this mapping?')) return;
                const mappingId = Number(b.dataset.deleteMapping);
                try {
                    await apiRequest(API.alerts.conditionMappings.byId(mappingId), { method: 'DELETE' });
                    await loadConditionMappings(state.selectedConditionType.id, state.selectedConditionType.name);
                    showNotice('Mapping deleted.');
                } catch (err) {
                    showNotice(err.message, true);
                }
            },
        }
    );
}

// ── Modals & CRUD ──────────────────────────────────────────────────────────

function toggleAlertTargetFields() {
    const isServer = $('alert-target-type').value === 'SERVER';
    $('alert-server-group').style.display  = isServer ? 'block' : 'none';
    $('alert-service-group').style.display = !isServer ? 'block' : 'none';
}

function openAlertModal(id = null) {
    $('alert-form').reset();
    $('alert-id').value       = id || '';
    $('alert-enabled').checked = true;

    if (id) {
        const alert = state.alerts.find((a) => a.id === id);
        if (!alert) return showNotice('Alert rule not found.', true);
        $('alert-name').value            = alert.name            || '';
        $('alert-target-type').value     = alert.targetType      || 'SERVER';
        $('alert-server').value          = alert.serverId        || '';
        $('alert-service').value         = alert.serviceId       || '';
        $('alert-condition-type').value  = alert.conditionType   || 'SERVICE_STATUS';
        $('alert-operator').value        = alert.operator        || 'EQUALS';
        $('alert-condition-value').value = alert.conditionValue  || '';
        $('alert-enabled').checked       = Boolean(alert.enabled);
    }

    toggleAlertTargetFields();
    $('alert-modal').classList.add('active');
}

async function deleteAlertRule(id) {
    if (!window.confirm('Delete this alert rule?')) return;
    try {
        await apiRequest(API.alerts.byId(id), { method: 'DELETE' });
        await loadAlertsData();
        showNotice('Alert rule deleted.');
    } catch (err) {
        showNotice('Failed to delete alert: ' + err.message, true);
    }
}

// ── Event Handlers ─────────────────────────────────────────────────────────

function initAlertsPage() {
    // Alert Tabs (Rules, Events, Conditions)
    document.querySelectorAll('.tab-btn[data-alert-tab]').forEach((btn) => {
        btn.onclick = () => {
            document.querySelectorAll('.tab-btn[data-alert-tab]').forEach((b) => b.classList.remove('active'));
            btn.classList.add('active');
            state.activeAlertTab = btn.dataset.alertTab;

            $('alert-tab-rules').style.display      = state.activeAlertTab === 'rules' ? 'block' : 'none';
            $('alert-tab-events').style.display     = state.activeAlertTab === 'events' ? 'block' : 'none';
            $('alert-tab-conditions').style.display = state.activeAlertTab === 'conditions' ? 'block' : 'none';

            loadAlertsData();
        };
    });

    // Notification dropdown
    $('notification-bell')?.addEventListener('click', (e) => {
        e.stopPropagation();
        $('notification-dropdown')?.classList.toggle('active');
    });

    document.addEventListener('click', (e) => {
        if (!e.target.closest('.notification-container')) {
            $('notification-dropdown')?.classList.remove('active');
        }
    });

    $('mark-all-read')?.addEventListener('click', async () => {
        try {
            await apiRequest(API.alerts.events.markAll, { method: 'PUT' });
            await loadAlertsData();
            showNotice('All alerts marked as read.');
        } catch (err) {
            showNotice('Failed to mark all as read: ' + err.message, true);
        }
    });

    $('alert-target-type')?.addEventListener('change', toggleAlertTargetFields);

    // Alert Rule Form
    $('alert-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const id       = $('alert-id').value;
        const isServer = $('alert-target-type').value === 'SERVER';

        const payload = {
            name:           $('alert-name').value.trim(),
            targetType:     $('alert-target-type').value,
            serverId:       isServer ? Number($('alert-server').value) : null,
            serviceId:      !isServer ? Number($('alert-service').value) : null,
            conditionType:  $('alert-condition-type').value,
            operator:       $('alert-operator').value,
            conditionValue: $('alert-condition-value').value.trim(),
            enabled:        $('alert-enabled').checked,
        };

        try {
            await apiRequest(id ? API.alerts.byId(id) : API.alerts.list, {
                method: id ? 'PUT' : 'POST',
                body:   JSON.stringify(payload),
            });
            $('alert-modal').classList.remove('active');
            await loadAlertsData();
            showNotice(id ? 'Alert rule updated.' : 'Alert rule created.');
        } catch (err) {
            showNotice('Failed to save alert rule: ' + err.message, true);
        }
    });

    // Log Event Modal
    $('open-log-event-modal')?.addEventListener('click', () => {
        $('log-event-form').reset();
        $('log-event-modal').classList.add('active');
    });

    $('log-event-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            alertId:          Number($('event-log-alert-id').value),
            serverId:         $('event-log-server').value ? Number($('event-log-server').value) : null,
            serviceId:        $('event-log-service-id').value ? Number($('event-log-service-id').value) : null,
            triggerCondition: $('event-log-condition').value.trim(),
            alertStatus:      $('event-log-status').value,
            alertMessage:     $('event-log-message').value.trim(),
        };

        try {
            await apiRequest(API.alerts.events.log, {
                method: 'POST',
                body:   JSON.stringify(payload),
            });
            $('log-event-modal').classList.remove('active');
            await loadAlertsData();
            showNotice('Alert event logged successfully.');
        } catch (err) {
            showNotice('Failed to log event: ' + err.message, true);
        }
    });

    // Add Condition Mapping Modal
    $('add-condition-mapping-btn')?.addEventListener('click', () => {
        if (!state.selectedConditionType) return;
        $('mapping-form').reset();
        $('mapping-modal').classList.add('active');
    });

    $('mapping-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            conditionKey:   $('mapping-key').value.trim(),
            conditionValue: $('mapping-value').value.trim(),
            description:    $('mapping-desc').value.trim(),
        };

        try {
            await apiRequest(API.alerts.conditionTypes.mappings(state.selectedConditionType.id), {
                method: 'POST',
                body:   JSON.stringify(payload),
            });
            $('mapping-modal').classList.remove('active');
            await loadConditionMappings(state.selectedConditionType.id, state.selectedConditionType.name);
            showNotice('Condition mapping created.');
        } catch (err) {
            showNotice('Failed to create mapping: ' + err.message, true);
        }
    });

    $('add-alert')?.addEventListener('click',      () => openAlertModal());
    $('refresh-alerts')?.addEventListener('click', loadAlertsData);
}
