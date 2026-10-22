/**
 * services.js — Services Management page and service modal module.
 *
 * Responsibilities:
 *  - Render the All-Services table view (with server filter)
 *  - Open, populate, and handle submission of the Service modal
 *  - Auto-create / sync path helpers (LOGS_HOME, SCRIPTS_HOME)
 *  - Delete services
 *
 * Depends on: config.js, api.js, ui.js, state.js, servers.js (selectServer, loadServers)
 */

'use strict';

// ── All-Services Table View ────────────────────────────────────────────────

// Issue #27: Load all server service configurations to display live Health Check URLs in Registry [Author: Ravindra Singh Pawar / Antigravity]
// Issue #28: Display Framework/Type and live Status badges in Service Registry [Author: Ravindra Singh Pawar / Antigravity]
async function loadAllServicesView() {
    const container = $('all-services-table');
    if (!container) return;

    let allConfigs = [];
    try {
        allConfigs = unwrap(await apiRequest(API.services.serverServiceConfigsAll, { silent: true })) || [];
        state.allServerServiceConfigs = allConfigs;
    } catch {
        allConfigs = state.allServerServiceConfigs || [];
    }

    const filterServerId = $('service-filter-server')?.value;
    const allServices = [];

    state.servers.forEach((server) => {
        if (filterServerId && String(server.id) !== filterServerId) return;
        (server.services || []).forEach((service) => {
            const config = allConfigs.find((c) => c.serviceId === service.id && c.serverId === server.id);
            allServices.push({
                ...service,
                server,
                serviceType: config?.serviceType || service.serviceType || 'OTHER',
                status: (state.healthStatuses && state.healthStatuses[service.id]) || config?.status || 'UNKNOWN',
                healthCheckUrl: config?.healthCheckUrl || service.healthCheckUrl || '',
            });
        });
    });

    const rows = allServices.map((svc) => {
        // Issue #28: Format framework / type badge
        const rawType = (svc.serviceType || 'OTHER').toUpperCase();
        let typeLabel = 'Other Service';
        let typeClass = 'other';
        if (rawType === 'SPRING_BOOT') {
            typeLabel = 'Spring Boot';
            typeClass = 'spring-boot';
        } else if (rawType === 'SYSTEMD') {
            typeLabel = 'Systemd';
            typeClass = 'systemd';
        }

        // Issue #28: Format live status pill badge
        const rawStatus = (svc.status || 'UNKNOWN').toUpperCase();
        const isUp = rawStatus === 'UP' || rawStatus === 'ONLINE';
        const isDown = rawStatus === 'DOWN' || rawStatus === 'OFFLINE';
        const statusLabel = isUp ? 'UP' : isDown ? 'DOWN' : 'UNKNOWN';
        const statusClass = isUp ? 'up' : isDown ? 'down' : 'unknown';

        return `
            <tr>
                <td><strong>#${svc.id}</strong></td>
                <td><strong>${escapeHtml(svc.serviceName)}</strong></td>
                <td>${escapeHtml(svc.server.hostname || svc.server.ipAddress)}</td>
                <td><span class="type-badge ${typeClass}">${escapeHtml(typeLabel)}</span></td>
                <td><span class="status ${statusClass}"><span class="pulse-dot"></span>${escapeHtml(statusLabel)}</span></td>
                <td><code>${escapeHtml(svc.healthCheckUrl || '-')}</code></td>
                <td>
                    <div class="service-actions">
                        <button class="btn secondary btn-sm" data-svc-dashboard="${svc.server.id}">View</button>
                        <button class="btn secondary btn-sm" data-svc-edit="${svc.id}" data-svc-server="${svc.server.id}">Edit</button>
                        <button class="btn danger btn-sm"    data-svc-delete="${svc.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `;
    });

    renderTable(
        container,
        ['ID', 'Service Name', 'Server', 'Type', 'Status', 'Health Check URL', 'Actions'],
        rows,
        'No services found for the selected server filter.',
        {
            '[data-svc-dashboard]': (b) => {
                selectServer(Number(b.dataset.svcDashboard));
                showPage('dashboard');
            },
            '[data-svc-edit]': async (b) => {
                const sId = Number(b.dataset.svcServer);
                const svcId = Number(b.dataset.svcEdit);
                if (sId) await selectServer(sId);
                await openServiceModal(svcId, sId);
            },
            '[data-svc-delete]': (b) => deleteService(Number(b.dataset.svcDelete)),
        }
    );
}

// ── Service Modal ──────────────────────────────────────────────────────────

// Issue #27: Dynamic Actuator base path helper [Author: Ravindra Singh Pawar / Antigravity]
function syncSpringBootHealthUrl(force = false) {
    if ($('service-type').value !== 'SPRING_BOOT') return;
    const urlInput = $('health-check-url');
    if (!urlInput) return;
    if (!force && urlInput.dataset.manualEdited === 'true') return;

    const serverId = Number($('service-server').value);
    const server   = state.servers.find((s) => s.id === serverId) || state.selectedServer;
    const host     = server?.ipAddress || server?.hostname || '127.0.0.1';
    const port     = Number($('actuator-port').value || 8080);
    const ctx      = ($('context-path').value || '').trim().replace(/^\/+|\/+$/g, '');
    urlInput.value = `http://${host}:${port}${ctx ? `/${ctx}` : ''}/actuator/health`;
}

// Issue #27/#31: Open, populate, and pre-fill Service modal with accurate Spring Boot type and Health Check URL [Author: Ravindra Singh Pawar / Antigravity]
async function openServiceModal(id = null, serverId = null) {
    if (serverId && (!state.selectedServer || Number(state.selectedServer.id) !== Number(serverId))) {
        await selectServer(serverId);
    }
    if (!state.selectedServer && state.servers && state.servers.length > 0) {
        await selectServer(state.servers[0].id);
    }
    if (!state.selectedServer) return showNotice('Please select a server first.', true);

    try {
        state.paths = unwrap(await apiRequest(API.paths.list, { silent: true })) || [];
    } catch {
        state.paths = [];
    }

    let allConfigs = [];
    try {
        allConfigs = unwrap(await apiRequest(API.services.serverServiceConfigsAll, { silent: true })) || [];
        state.allServerServiceConfigs = allConfigs;
    } catch {
        allConfigs = state.allServerServiceConfigs || [];
    }

    $('service-form').reset();
    $('service-id').value = id || '';
    if (typeof renderServerSelectDropdowns === 'function') {
        renderServerSelectDropdowns();
    }

    const urlInput = $('health-check-url');
    if (urlInput) {
        urlInput.dataset.manualEdited = 'false';
    }

    let service = null;
    let targetServer = state.selectedServer;

    if (id) {
        // 1. Locate service in target server
        if (targetServer?.services) {
            service = targetServer.services.find((s) => Number(s.id) === Number(id));
        }
        // 2. Search across all servers
        if (!service && state.servers) {
            for (const s of state.servers) {
                const found = (s.services || []).find((svc) => Number(svc.id) === Number(id));
                if (found) {
                    service = found;
                    targetServer = s;
                    state.selectedServer = s;
                    break;
                }
            }
        }
        // 3. Direct API fetch fallback
        if (!service) {
            try {
                service = unwrap(await apiRequest(API.services.byId(id), { silent: true }));
                if (service?.servers?.length > 0) {
                    const sId = service.servers[0].id;
                    targetServer = state.servers.find((s) => Number(s.id) === Number(sId)) || targetServer;
                }
            } catch {}
        }
    }

    if (targetServer && $('service-server')) {
        $('service-server').value = targetServer.id;
    }

    if (service) {
        $('service-name').value = service.serviceName || '';
        const config = allConfigs.find((c) => Number(c.serviceId) === Number(id) && (!targetServer || Number(c.serverId) === Number(targetServer.id)))
                    || allConfigs.find((c) => Number(c.serviceId) === Number(id))
                    || (state.serverServiceConfigurations || []).find((c) => Number(c.serviceId) === Number(id));

        const healthUrl = config?.healthCheckUrl || service.healthCheckUrl || '';
        const rawType = (config?.serviceType || service.serviceType || '').toUpperCase();
        let serviceTypeValue = 'SPRING_BOOT';
        if (rawType === 'SYSTEMD') {
            serviceTypeValue = 'SYSTEMD';
        } else if (rawType === 'OTHER') {
            serviceTypeValue = 'OTHER';
        } else if (rawType === 'SPRING_BOOT' || healthUrl.includes('/actuator/health') || !rawType) {
            serviceTypeValue = 'SPRING_BOOT';
        }
        $('service-type').value = serviceTypeValue;

        const serverPaths = state.paths.filter((p) =>
            p.servers?.some((s) => Number(s.id) === Number(targetServer?.id))
        );
        const logsPath    = config?.logsPath || serverPaths.find((p) => p.pathName === 'LOGS_HOME')?.pathDescription || '';
        const scriptsPath = config?.scriptsPath || serverPaths.find((p) => p.pathName === 'SCRIPTS_HOME')?.pathDescription || '';
        $('logs-path').value    = logsPath;
        $('scripts-path').value = scriptsPath;

        const isSpringBoot = serviceTypeValue === 'SPRING_BOOT';
        $('spring-boot-fields').style.display = isSpringBoot ? 'block' : 'none';

        if (healthUrl) {
            if (urlInput) {
                urlInput.value = healthUrl;
                urlInput.dataset.manualEdited = 'true';
            }
            try {
                const parsed = new URL(healthUrl);
                $('actuator-port').value = parsed.port || (parsed.protocol === 'https:' ? '443' : '80');
                $('context-path').value  = parsed.pathname.replace(/\/actuator\/health\/?$/, '');
            } catch {
                $('actuator-port').value = config?.actuatorPort || '8080';
                $('context-path').value  = config?.contextPath || '';
            }
        } else {
            $('actuator-port').value = config?.actuatorPort || '8080';
            $('context-path').value  = config?.contextPath || '';
            if (isSpringBoot) {
                syncSpringBootHealthUrl(true);
            }
        }
    } else {
        $('service-type').value  = 'SPRING_BOOT';
        $('actuator-port').value = '8080';
        $('context-path').value  = '';
        $('spring-boot-fields').style.display = 'block';
        syncSpringBootHealthUrl(true);
    }

    $('service-modal').classList.add('active');
}

// ── Path Helper ────────────────────────────────────────────────────────────

async function createPathHelper(server, pathName, pathValue) {
    try {
        await apiRequest(API.paths.list, {
            method: 'POST',
            body: JSON.stringify({
                id: null,
                pathName,
                pathDescription: pathValue,
                servers: [{ id: server.id }],
                scripts: [],
                createdSource: 'UI',
                lastUpdatedSource: 'UI',
            }),
            silent: true,
        });
    } catch {
        // Ignored if already exists
    }
}

// ── Delete Service ─────────────────────────────────────────────────────────

async function deleteService(id) {
    if (!window.confirm('Are you sure you want to delete this service?')) return;
    try {
        await apiRequest(API.services.byId(id), { method: 'DELETE' });
        await loadServers();
        if (state.selectedServer) await selectServer(state.selectedServer.id);
        if (state.activePage === 'services') loadAllServicesView();
        showNotice('Service deleted successfully.');
    } catch (err) {
        showNotice('Failed to delete service: ' + err.message, true);
    }
}

// ── Form & Event Handlers ──────────────────────────────────────────────────

function initServicesPage() {
    $('service-type')?.addEventListener('change', (e) => {
        const isSpringBoot = e.target.value === 'SPRING_BOOT';
        $('spring-boot-fields').style.display = isSpringBoot ? 'block' : 'none';
        if (isSpringBoot) {
            syncSpringBootHealthUrl(true);
        }
    });

    $('actuator-port')?.addEventListener('input', () => syncSpringBootHealthUrl(false));
    $('context-path')?.addEventListener('input', () => syncSpringBootHealthUrl(false));
    $('service-server')?.addEventListener('change', () => syncSpringBootHealthUrl(false));
    $('health-check-url')?.addEventListener('input', (e) => {
        e.target.dataset.manualEdited = 'true';
    });

    $('service-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const serverId = Number($('service-server').value);
        const server   = state.servers.find((s) => s.id === serverId);
        if (!server) return showNotice('Select a valid server.', true);

        // Issue #27: Read configured Health Check URL directly from modal [Author: Ravindra Singh Pawar / Antigravity]
        let healthCheckUrl = ($('health-check-url')?.value || '').trim();
        if (!healthCheckUrl && $('service-type').value === 'SPRING_BOOT') {
            const host = server.ipAddress || server.hostname || '127.0.0.1';
            const port = Number($('actuator-port').value || 8080);
            const ctx  = ($('context-path').value || '').trim().replace(/^\/+|\/+$/g, '');
            healthCheckUrl = `http://${host}:${port}${ctx ? `/${ctx}` : ''}/actuator/health`;
        }

        const id = $('service-id').value;
        const serviceType = $('service-type')?.value || 'SPRING_BOOT';
        const payload = {
            id: id ? Number(id) : null,
            servers: [server],
            serviceName: $('service-name').value.trim(),
            serviceType,
            healthCheckUrl,
            createdSource: 'UI',
            lastUpdatedSource: 'UI',
        };

        try {
            await apiRequest(id ? API.services.byId(id) : API.services.list, {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload),
            });

            // Auto-create/sync path helpers for LOGS_HOME and SCRIPTS_HOME
            if ($('logs-path').value.trim()) {
                await createPathHelper(server, 'LOGS_HOME', $('logs-path').value.trim());
            }
            if ($('scripts-path').value.trim()) {
                await createPathHelper(server, 'SCRIPTS_HOME', $('scripts-path').value.trim());
            }

            $('service-modal').classList.remove('active');
            await loadServers();
            await selectServer(server.id);
            if (state.activePage === 'services') loadAllServicesView();
            showNotice(id ? 'Service updated successfully.' : 'Service created successfully.');
        } catch (err) {
            showNotice('Failed to save service: ' + err.message, true);
        }
    });

    $('add-service')?.addEventListener('click',      () => openServiceModal());
    $('add-service-page')?.addEventListener('click', () => openServiceModal());
    $('service-filter-server')?.addEventListener('change', loadAllServicesView);
}
