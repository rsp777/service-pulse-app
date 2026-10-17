(() => {
    'use strict';

    // Application Global State
    const state = {
        servers: [],
        selectedServer: null,
        commands: [],
        paths: [],
        scripts: [],
        alerts: [],
        alertEvents: [],
        unreadAlerts: [],
        conditionTypes: [],
        selectedConditionType: null,
        conditionMappings: [],
        healthStatuses: {},
        serverServiceConfigurations: [],
        activePage: 'dashboard',
        activeAlertTab: 'rules',
        systemLogs: [],
        isLogStreamPaused: false,
        autoScrollLogs: true,
        logMinLevel: 'ALL'
    };

    let logStreamSource = null;
    let serviceActionStreamSource = null;
    let systemLogStreamSource = null;

    const $ = (id) => document.getElementById(id);
    const unwrap = (val) => val?.data ?? val;

    const escapeHtml = (str) =>
        String(str ?? '').replace(/[&<>"']/g, (m) => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#039;'
        })[m]);

    const getBaseUrl = () => {
        const baseEl = document.querySelector('base');
        if (baseEl && baseEl.getAttribute('href')) {
            return baseEl.getAttribute('href').replace(/\/+$/, '') + '/';
        }
        return '';
    };

    const apiUrl = (endpoint) => {
        const clean = endpoint.replace(/^\/+/, '');
        return getBaseUrl() + clean;
    };

    const showNotice = (text, isError = false) => {
        const el = $('notice');
        if (!el) return;
        el.textContent = text;
        el.className = 'visible' + (isError ? ' error' : '');
        clearTimeout(el._timer);
        el._timer = setTimeout(() => {
            el.className = '';
            el.textContent = '';
        }, 4500);
    };

    const apiRequest = async (endpoint, options = {}) => {
        const url = apiUrl(endpoint);
        const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
        const response = await fetch(url, { ...options, headers });
        if (!response.ok) {
            const body = await response.text();
            try {
                const payload = JSON.parse(body);
                const error = new Error(payload.message || `${response.status} Request Failed`);
                error.code = payload.code;
                throw error;
            } catch (err) {
                if (err instanceof SyntaxError) throw new Error(body || `${response.status} Request Failed`);
                throw err;
            }
        }
        if (response.status === 204) return null;
        const payload = await response.json();
        if (payload?.message && !options.silent) {
            showNotice(payload.message, payload.success === false);
        }
        return payload;
    };

    // Navigation and Page Routing
    const showPage = (pageName) => {
        state.activePage = pageName;
        document.querySelectorAll('.page').forEach((el) => {
            el.classList.toggle('active', el.id === pageName);
        });
        document.querySelectorAll('.nav button').forEach((btn) => {
            btn.classList.toggle('active', btn.dataset.page === pageName);
        });

        // Update URL hash without reload
        if (window.location.hash.replace(/^#/, '') !== pageName) {
            window.location.hash = pageName;
        }

        // Trigger page-specific loads
        if (pageName === 'dashboard') {
            updateDashboardStats();
            if (state.selectedServer) loadServices();
        } else if (pageName === 'servers') {
            renderServersTable();
        } else if (pageName === 'services') {
            loadAllServicesView();
        } else if (pageName === 'commands') {
            loadCommands();
        } else if (pageName === 'alerts') {
            loadAlertsData();
        } else if (pageName === 'logs') {
            initSystemLogs();
        } else if (pageName === 'settings') {
            loadSettings();
        }
    };

    const initHashRouting = () => {
        const handleHash = () => {
            const hash = window.location.hash.replace(/^#/, '');
            const validPages = ['dashboard', 'servers', 'services', 'commands', 'alerts', 'logs', 'settings'];
            if (validPages.includes(hash)) {
                showPage(hash);
            } else {
                showPage('dashboard');
            }
        };
        window.addEventListener('hashchange', handleHash);
        handleHash();
    };

    // Dashboard Stats
    const updateDashboardStats = () => {
        const totalServers = state.servers.length;
        let totalServices = 0;
        let onlineServices = 0;
        state.servers.forEach((s) => {
            const list = s.services || [];
            totalServices += list.length;
            list.forEach((svc) => {
                if (state.healthStatuses[svc.id] === 'UP') onlineServices++;
            });
        });

        if ($('stat-servers-count')) $('stat-servers-count').textContent = totalServers;
        if ($('stat-services-count')) $('stat-services-count').textContent = `${onlineServices} / ${totalServices}`;
        if ($('stat-alerts-count')) $('stat-alerts-count').textContent = state.unreadAlerts.length;
        if ($('stat-ssh-status')) $('stat-ssh-status').textContent = totalServers > 0 ? 'Active' : 'Idle';
    };

    // Servers Management
    const loadServers = async () => {
        try {
            state.servers = unwrap(await apiRequest('api/servers', { silent: true })) || [];
            renderServerSelectDropdowns();
            renderServersTable();
            updateDashboardStats();

            // Select initial or maintain current selection
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
    };

    const renderServerSelectDropdowns = () => {
        const optionsHtml = state.servers
            .map((s) => `<option value="${s.id}">${escapeHtml(s.hostname || s.ipAddress)} (${escapeHtml(s.ipAddress || '')})</option>`)
            .join('');

        if ($('server-select')) {
            $('server-select').innerHTML = `<option value="">-- Select a Server --</option>${optionsHtml}`;
            if (state.selectedServer) $('server-select').value = state.selectedServer.id;
        }
        if ($('service-server')) {
            $('service-server').innerHTML = `<option value="">-- Select Server --</option>${optionsHtml}`;
        }
        if ($('service-filter-server')) {
            $('service-filter-server').innerHTML = `<option value="">All Servers</option>${optionsHtml}`;
        }
        if ($('alert-server')) {
            $('alert-server').innerHTML = `<option value="">Select Server</option>${optionsHtml}`;
        }
        if ($('event-log-server')) {
            $('event-log-server').innerHTML = `<option value="">Optional Server</option>${optionsHtml}`;
        }
        if ($('path-server')) {
            $('path-server').innerHTML = `<option value="">Select Server</option>${optionsHtml}`;
        }
    };

    const selectServer = async (id) => {
        if (!id) {
            state.selectedServer = null;
            if ($('selected-title')) $('selected-title').textContent = 'Select a server';
            if ($('selected-meta')) $('selected-meta').textContent = 'Choose a server to view and control its services.';
            if ($('service-list')) $('service-list').innerHTML = '<div class="empty-state">Select a server to load services.</div>';
            return;
        }

        const server = state.servers.find((s) => s.id === id);
        if (!server) return;
        state.selectedServer = server;
        if ($('server-select')) $('server-select').value = server.id;

        if ($('selected-title')) $('selected-title').textContent = server.hostname || server.ipAddress;
        if ($('selected-meta')) {
            $('selected-meta').innerHTML = `IP: <strong>${escapeHtml(server.ipAddress || 'N/A')}</strong> · OS: <strong>${escapeHtml(server.osType || 'Linux')}</strong> · Status: <span class="status ${(server.status || '').toLowerCase()}"><span class="pulse-dot"></span>${escapeHtml(server.status || 'UNKNOWN')}</span>`;
        }

        try {
            const persisted = unwrap(await apiRequest(`api/service-health-status/server/${id}`, { silent: true })) || [];
            state.healthStatuses = Object.fromEntries(persisted.map((item) => [item.service?.id, item.status]));
        } catch {
            state.healthStatuses = {};
        }

        try {
            state.serverServiceConfigurations = unwrap(await apiRequest(`api/server-service-configurations/server/${id}`, { silent: true })) || [];
        } catch {
            state.serverServiceConfigurations = [];
        }

        await loadServices();
        updateDashboardStats();
    };

    const renderServersTable = () => {
        const container = $('server-table');
        if (!container) return;

        if (!state.servers.length) {
            container.innerHTML = '<div class="empty-state">No servers registered. Click "+ Add server" to add your first server.</div>';
            return;
        }

        const rows = state.servers.map((s) => {
            const statusClass = (s.status || 'UNKNOWN').toLowerCase();
            const serviceCount = s.services?.length || 0;
            return `
                <tr>
                    <td><strong>#${s.id}</strong></td>
                    <td><strong>${escapeHtml(s.hostname || 'Unnamed')}</strong></td>
                    <td><code>${escapeHtml(s.ipAddress || '')}</code></td>
                    <td>${escapeHtml(s.osType || 'Linux')}</td>
                    <td><span class="status ${statusClass}"><span class="pulse-dot"></span>${escapeHtml(s.status || 'UNKNOWN')}</span></td>
                    <td>${serviceCount} service(s)</td>
                    <td>
                        <div class="service-actions">
                            <button class="btn secondary btn-sm" data-server-select="${s.id}">Dashboard</button>
                            <button class="btn secondary btn-sm" data-server-edit="${s.id}">Edit</button>
                            <button class="btn danger btn-sm" data-server-delete="${s.id}">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        container.innerHTML = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Hostname</th>
                            <th>IP Address</th>
                            <th>OS</th>
                            <th>Status</th>
                            <th>Services</th>
                            <th style="text-align: right;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;

        container.querySelectorAll('[data-server-select]').forEach((b) => {
            b.onclick = () => {
                selectServer(Number(b.dataset.serverSelect));
                showPage('dashboard');
            };
        });
        container.querySelectorAll('[data-server-edit]').forEach((b) => {
            b.onclick = () => openServerModal(Number(b.dataset.serverEdit));
        });
        container.querySelectorAll('[data-server-delete]').forEach((b) => {
            b.onclick = () => deleteServer(Number(b.dataset.serverDelete));
        });
    };

    const openServerModal = (id = null) => {
        $('server-form').reset();
        $('server-id').value = id || '';
        $('os-type').value = 'Linux';
        if (id) {
            const server = state.servers.find((s) => s.id === id);
            if (!server) return showNotice('Server not found.', true);
            $('hostname').value = server.hostname || '';
            $('ip-address').value = server.ipAddress || '';
            $('os-type').value = server.osType || 'Linux';
            $('server-status').value = server.status || 'ONLINE';
        }
        $('server-modal').classList.add('active');
    };

    const deleteServer = async (id) => {
        if (!window.confirm('Are you sure you want to delete this server?')) return;
        try {
            await apiRequest(`api/servers/${id}`, { method: 'DELETE' });
            if (state.selectedServer?.id === id) state.selectedServer = null;
            await loadServers();
            showNotice('Server deleted successfully.');
        } catch (err) {
            showNotice('Failed to delete server: ' + err.message, true);
        }
    };

    $('server-form').onsubmit = async (e) => {
        e.preventDefault();
        const id = $('server-id').value;
        const payload = {
            id: id ? Number(id) : null,
            hostname: $('hostname').value.trim(),
            ipAddress: $('ip-address').value.trim(),
            osType: $('os-type').value.trim(),
            status: $('server-status').value,
            paths: [],
            services: [],
            createdSource: 'UI',
            lastUpdatedSource: 'UI'
        };
        try {
            await apiRequest(id ? `api/servers/${id}` : 'api/servers', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            $('server-modal').classList.remove('active');
            await loadServers();
            showNotice(id ? 'Server updated successfully.' : 'Server created successfully.');
        } catch (err) {
            showNotice('Failed to save server: ' + err.message, true);
        }
    };

    // Services Management
    const getServiceHealthUrl = (service) => {
        return state.serverServiceConfigurations.find((c) => c.serviceId === service.id)?.healthCheckUrl || '';
    };

    const updateServiceRuntime = async (service) => {
        const versionEl = $(`version-${service.id}`);
        const indicatorEl = $(`indicator-${service.id}`);
        const healthUrl = getServiceHealthUrl(service);
        if (!healthUrl) return;

        const baseUrl = healthUrl.replace(/\/actuator\/health\/?$/, '');
        try {
            const health = await fetch(`${baseUrl}/actuator/health`, { signal: AbortSignal.timeout(4000) });
            if (indicatorEl) {
                indicatorEl.className = `status ${health.ok ? 'online' : 'offline'}`;
                indicatorEl.innerHTML = `<span class="pulse-dot"></span>${health.ok ? 'ONLINE' : 'OFFLINE'}`;
            }
            const info = await fetch(`${baseUrl}/actuator/info`, { signal: AbortSignal.timeout(4000) });
            if (info.ok && versionEl) {
                const data = await info.json();
                versionEl.textContent = data?.build?.version || data?.build?.project?.version || 'Active';
            }
        } catch {
            if (indicatorEl) {
                indicatorEl.className = 'status offline';
                indicatorEl.innerHTML = '<span class="pulse-dot"></span>OFFLINE';
            }
        }
    };

    const loadServices = async () => {
        if (!state.selectedServer) return;
        const services = state.selectedServer.services || [];
        const hasMultiple = services.length > 1;

        ['start-all-services', 'stop-all-services', 'restart-all-services'].forEach((btnId) => {
            const btn = $(btnId);
            if (btn) btn.hidden = !hasMultiple;
        });

        const listContainer = $('service-list');
        if (!listContainer) return;

        if (!services.length) {
            listContainer.innerHTML = '<div class="empty-state">No services are associated with this server. Click "+ Add service" above to configure one.</div>';
            return;
        }

        listContainer.innerHTML = services.map((service) => {
            const healthUrl = getServiceHealthUrl(service);
            const statusVal = state.healthStatuses[service.id] || 'UNKNOWN';
            const statusClass = statusVal === 'UP' ? 'online' : statusVal === 'DOWN' ? 'offline' : 'unknown';

            return `
                <div class="service-row" data-service-id="${service.id}">
                    <div class="service-info">
                        <strong>${escapeHtml(service.serviceName)}</strong>
                        <small>Health URL: ${escapeHtml(healthUrl || 'Not configured')}</small>
                        <small>Version: <span id="version-${service.id}">Unknown</span> · Status: <span id="indicator-${service.id}" class="status ${statusClass}"><span class="pulse-dot"></span>${escapeHtml(statusVal)}</span></small>
                    </div>
                    <div class="service-actions">
                        <button class="btn success btn-sm" data-service-action="start">Start</button>
                        <button class="btn secondary btn-sm" data-service-action="stop">Stop</button>
                        <button class="btn secondary btn-sm" data-service-action="restart">Restart</button>
                        <button class="btn secondary btn-sm" data-service-action="logs">Logs</button>
                        <button class="btn secondary btn-sm" data-service-edit="${service.id}">Edit</button>
                        <button class="btn danger btn-sm" data-service-delete="${service.id}">Delete</button>
                    </div>
                </div>
            `;
        }).join('');

        listContainer.querySelectorAll('[data-service-action]').forEach((btn) => {
            btn.onclick = () => {
                const action = btn.dataset.serviceAction;
                const serviceId = Number(btn.closest('[data-service-id]').dataset.serviceId);
                serviceAction(action, serviceId);
            };
        });

        listContainer.querySelectorAll('[data-service-edit]').forEach((btn) => {
            btn.onclick = () => openServiceModal(Number(btn.dataset.serviceEdit));
        });

        listContainer.querySelectorAll('[data-service-delete]').forEach((btn) => {
            btn.onclick = () => deleteService(Number(btn.dataset.serviceDelete));
        });

        services.forEach((s) => updateServiceRuntime(s));
    };

    const loadAllServicesView = () => {
        const container = $('all-services-table');
        if (!container) return;

        const filterServerId = $('service-filter-server')?.value;
        const allServices = [];

        state.servers.forEach((server) => {
            if (filterServerId && String(server.id) !== filterServerId) return;
            (server.services || []).forEach((service) => {
                allServices.push({ ...service, server });
            });
        });

        if (!allServices.length) {
            container.innerHTML = '<div class="empty-state">No services found for the selected server filter.</div>';
            return;
        }

        const rows = allServices.map((svc) => `
            <tr>
                <td><strong>#${svc.id}</strong></td>
                <td><strong>${escapeHtml(svc.serviceName)}</strong></td>
                <td>${escapeHtml(svc.server.hostname || svc.server.ipAddress)}</td>
                <td><code>${escapeHtml(svc.healthCheckUrl || '-')}</code></td>
                <td>
                    <div class="service-actions">
                        <button class="btn secondary btn-sm" data-svc-dashboard="${svc.server.id}">View</button>
                        <button class="btn secondary btn-sm" data-svc-edit="${svc.id}" data-svc-server="${svc.server.id}">Edit</button>
                        <button class="btn danger btn-sm" data-svc-delete="${svc.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Service Name</th>
                            <th>Server</th>
                            <th>Health Check URL</th>
                            <th style="text-align: right;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;

        container.querySelectorAll('[data-svc-dashboard]').forEach((b) => {
            b.onclick = () => {
                selectServer(Number(b.dataset.svcDashboard));
                showPage('dashboard');
            };
        });
        container.querySelectorAll('[data-svc-edit]').forEach((b) => {
            b.onclick = () => {
                selectServer(Number(b.dataset.svcServer));
                openServiceModal(Number(b.dataset.svcEdit));
            };
        });
        container.querySelectorAll('[data-svc-delete]').forEach((b) => {
            b.onclick = () => deleteService(Number(b.dataset.svcDelete));
        });
    };

    const openServiceModal = async (id = null) => {
        if (!state.selectedServer) return showNotice('Please select a server first.', true);

        try {
            state.paths = unwrap(await apiRequest('api/paths', { silent: true })) || [];
        } catch {
            state.paths = [];
        }

        $('service-form').reset();
        $('service-id').value = id || '';
        $('service-server').value = state.selectedServer.id;

        const service = id ? state.selectedServer.services?.find((s) => s.id === id) : null;
        if (service) {
            $('service-name').value = service.serviceName || '';
            const config = state.serverServiceConfigurations.find((c) => c.serviceId === service.id);
            const healthUrl = config?.healthCheckUrl || '';
            $('service-type').value = healthUrl ? 'SPRING_BOOT' : 'OTHER';

            const serverPaths = state.paths.filter((p) =>
                p.servers?.some((s) => Number(s.id) === Number(state.selectedServer.id))
            );
            const logsPath = serverPaths.find((p) => p.pathName === 'LOGS_HOME');
            const scriptsPath = serverPaths.find((p) => p.pathName === 'SCRIPTS_HOME');
            $('logs-path').value = logsPath?.pathDescription || '';
            $('scripts-path').value = scriptsPath?.pathDescription || '';

            if (healthUrl) {
                try {
                    const parsed = new URL(healthUrl);
                    $('actuator-port').value = parsed.port || '80';
                    $('context-path').value = parsed.pathname.replace(/\/actuator\/health\/?$/, '');
                } catch {
                    $('actuator-port').value = '8080';
                    $('context-path').value = '';
                }
            }
        } else {
            $('service-type').value = 'SPRING_BOOT';
            $('actuator-port').value = '8080';
            $('context-path').value = '';
        }

        $('spring-boot-fields').style.display = $('service-type').value === 'SPRING_BOOT' ? 'block' : 'none';
        $('service-modal').classList.add('active');
    };

    $('service-type').onchange = (e) => {
        $('spring-boot-fields').style.display = e.target.value === 'SPRING_BOOT' ? 'block' : 'none';
    };

    $('service-form').onsubmit = async (e) => {
        e.preventDefault();
        const serverId = Number($('service-server').value);
        const server = state.servers.find((s) => s.id === serverId);
        if (!server) return showNotice('Select a valid server.', true);

        const isSpringBoot = $('service-type').value === 'SPRING_BOOT';
        let healthCheckUrl = '';
        if (isSpringBoot) {
            const host = server.ipAddress || server.hostname;
            const port = Number($('actuator-port').value || 8080);
            const ctx = ($('context-path').value || '').trim().replace(/^\/+|\/+$/g, '');
            healthCheckUrl = `http://${host}:${port}${ctx ? `/${ctx}` : ''}/actuator/health`;
        }

        const id = $('service-id').value;
        const payload = {
            id: id ? Number(id) : null,
            servers: [server],
            serviceName: $('service-name').value.trim(),
            healthCheckUrl,
            createdSource: 'UI',
            lastUpdatedSource: 'UI'
        };

        try {
            await apiRequest(id ? `api/services/${id}` : 'api/services', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });

            // Auto-create/sync path helpers
            if ($('logs-path').value.trim()) {
                await createPathHelper(server, 'LOGS_HOME', $('logs-path').value.trim());
            }
            if ($('scripts-path').value.trim()) {
                await createPathHelper(server, 'SCRIPTS_HOME', $('scripts-path').value.trim());
            }

            $('service-modal').classList.remove('active');
            await loadServers();
            await selectServer(server.id);
            showNotice(id ? 'Service updated successfully.' : 'Service created successfully.');
        } catch (err) {
            showNotice('Failed to save service: ' + err.message, true);
        }
    };

    const deleteService = async (id) => {
        if (!window.confirm('Are you sure you want to delete this service?')) return;
        try {
            await apiRequest(`api/services/${id}`, { method: 'DELETE' });
            await loadServers();
            if (state.selectedServer) await selectServer(state.selectedServer.id);
            if (state.activePage === 'services') loadAllServicesView();
            showNotice('Service deleted successfully.');
        } catch (err) {
            showNotice('Failed to delete service: ' + err.message, true);
        }
    };

    const createPathHelper = async (server, pathName, pathValue) => {
        try {
            await apiRequest('api/paths', {
                method: 'POST',
                body: JSON.stringify({
                    id: null,
                    pathName,
                    pathDescription: pathValue,
                    servers: [{ id: server.id }],
                    scripts: [],
                    createdSource: 'UI',
                    lastUpdatedSource: 'UI'
                }),
                silent: true
            });
        } catch {
            // Ignored if already exists
        }
    };

    // Service Actions & Log Streaming
    const serviceAction = async (action, serviceId) => {
        if (action === 'logs') {
            streamServiceLogs(serviceId);
            return;
        }

        try {
            showNotice(`Executing ${action} on service #${serviceId}...`);
            const res = await apiRequest(`api/manage-services/${action}-service?serverId=${state.selectedServer.id}&serviceId=${serviceId}`, {
                method: 'POST'
            });

            const unwrapped = unwrap(res);
            let outputText = '';
            if (typeof unwrapped === 'object') {
                outputText = unwrapped.output || unwrapped.payload?.output || JSON.stringify(unwrapped, null, 2);
            } else {
                outputText = String(unwrapped);
            }

            const terminal = $('output');
            if (terminal) terminal.textContent = outputText.replace(/\\n/g, '\n').replace(/\\r/g, '');
            showNotice(`${action} completed successfully.`);
            await selectServer(state.selectedServer.id);
        } catch (err) {
            showNotice(`Service ${action} failed: ${err.message}`, true);
        }
    };

    const streamServiceLogs = (serviceId) => {
        if (logStreamSource) {
            logStreamSource.close();
            logStreamSource = null;
        }

        const terminal = $('logs-output');
        terminal.textContent = 'Connecting to log stream...\n';
        $('logs-modal').classList.add('active');

        const streamUrl = apiUrl(`api/logs/stream?serverId=${state.selectedServer.id}&serviceId=${serviceId}`);
        logStreamSource = new EventSource(streamUrl);

        logStreamSource.addEventListener('log', (event) => {
            terminal.textContent += `${event.data}\n`;
            terminal.scrollTop = terminal.scrollHeight;
        });

        logStreamSource.onerror = () => {
            if (logStreamSource) {
                logStreamSource.close();
                logStreamSource = null;
            }
            terminal.textContent += '\n[Stream disconnected]\n';
        };
    };

    const bulkServiceAction = async (action) => {
        if (!state.selectedServer) return showNotice('Select a server first.', true);
        const serverId = state.selectedServer.id;

        if (serviceActionStreamSource) {
            serviceActionStreamSource.close();
            serviceActionStreamSource = null;
        }

        const terminal = $('output');
        terminal.textContent = `Running ${action} all services on server #${serverId}...\n`;

        const streamUrl = apiUrl(`api/manage-services/${action}-all-service/stream?serverId=${serverId}`);
        serviceActionStreamSource = new EventSource(streamUrl);

        serviceActionStreamSource.addEventListener('output', (event) => {
            terminal.textContent += `${event.data}\n`;
            terminal.scrollTop = terminal.scrollHeight;
        });

        serviceActionStreamSource.addEventListener('complete', async () => {
            if (serviceActionStreamSource) {
                serviceActionStreamSource.close();
                serviceActionStreamSource = null;
            }
            terminal.textContent += `\n[${action.toUpperCase()} ALL COMPLETED]\n`;
            await selectServer(serverId);
            showNotice(`${action} all services completed.`);
        });

        serviceActionStreamSource.onerror = () => {
            if (serviceActionStreamSource) {
                serviceActionStreamSource.close();
                serviceActionStreamSource = null;
            }
            terminal.textContent += '\n[Lifecycle stream closed or completed]\n';
        };
    };

    // SSH Command Execution
    const loadCommands = async () => {
        try {
            state.commands = unwrap(await apiRequest('api/commands', { silent: true })) || [];
            renderCommandsSelect();
            renderCommandsTable();
        } catch (err) {
            showNotice('Failed to load commands: ' + err.message, true);
        }
    };

    const renderCommandsSelect = () => {
        const select = $('command-select');
        if (!select) return;
        const currentVal = select.value;
        select.innerHTML = `<option value="">Custom command</option>` +
            state.commands.map((c) => `<option value="${c.id}">${escapeHtml(c.description || c.name)}</option>`).join('');
        if (state.commands.some((c) => String(c.id) === currentVal)) {
            select.value = currentVal;
        }
    };

    const renderCommandsTable = () => {
        const container = $('command-table');
        if (!container) return;

        if (!state.commands.length) {
            container.innerHTML = '<div class="empty-state">No configured commands. Click "+ Add command" to create reusable SSH commands.</div>';
            return;
        }

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
                        <button class="btn danger btn-sm" data-cmd-delete="${c.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Command Name</th>
                            <th>Parameters / Arguments</th>
                            <th>Description</th>
                            <th>Status</th>
                            <th style="text-align: right;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;

        container.querySelectorAll('[data-cmd-run]').forEach((b) => {
            b.onclick = () => {
                const cmd = state.commands.find((c) => c.id === Number(b.dataset.cmdRun));
                if (cmd) {
                    const fullCmd = [cmd.name, cmd.parameters].filter(Boolean).join(' ');
                    $('command').value = fullCmd;
                    showPage('dashboard');
                    $('command-form').requestSubmit();
                }
            };
        });
        container.querySelectorAll('[data-cmd-edit]').forEach((b) => {
            b.onclick = () => openCommandModal(Number(b.dataset.cmdEdit));
        });
        container.querySelectorAll('[data-cmd-delete]').forEach((b) => {
            b.onclick = () => deleteCommand(Number(b.dataset.cmdDelete));
        });
    };

    $('command-select').onchange = (e) => {
        const selectedId = e.target.value;
        if (!selectedId) return;
        const cmd = state.commands.find((c) => String(c.id) === selectedId);
        if (cmd) {
            $('command').value = [cmd.name, cmd.parameters].filter(Boolean).join(' ');
        }
    };

    $('command-form').onsubmit = async (e) => {
        e.preventDefault();
        if (!state.selectedServer) return showNotice('Select a server first.', true);
        const cmdText = $('command').value.trim();
        if (!cmdText) return showNotice('Please enter a command to execute.', true);

        const terminal = $('output');
        terminal.textContent = `Executing: ${cmdText} on ${state.selectedServer.hostname || state.selectedServer.ipAddress}...\n`;

        try {
            const res = await apiRequest(`api/commands/execute?serverId=${state.selectedServer.id}&command=${encodeURIComponent(cmdText)}`, {
                method: 'POST'
            });
            const data = unwrap(res);
            terminal.textContent = typeof data === 'object' ? JSON.stringify(data, null, 2) : String(data);
            terminal.scrollTop = terminal.scrollHeight;
            showNotice('Command completed.');
        } catch (err) {
            terminal.textContent += `\n[ERROR]: ${err.message}`;
            showNotice('Command failed: ' + err.message, true);
        }
    };

    const openCommandModal = (id = null) => {
        $('command-crud-form').reset();
        $('command-id').value = id || '';
        if (id) {
            const cmd = state.commands.find((c) => c.id === id);
            if (!cmd) return showNotice('Command not found.', true);
            $('command-name').value = cmd.name || '';
            $('command-description').value = cmd.description || '';
            $('command-parameters').value = cmd.parameters || '';
            $('command-status').value = cmd.status || 'CREATED';
            $('command-result').value = cmd.result || '';
        } else {
            $('command-status').value = 'CREATED';
        }
        $('command-modal').classList.add('active');
    };

    $('command-crud-form').onsubmit = async (e) => {
        e.preventDefault();
        const id = $('command-id').value;
        const payload = {
            name: $('command-name').value.trim(),
            description: $('command-description').value.trim(),
            parameters: $('command-parameters').value.trim(),
            status: $('command-status').value,
            result: $('command-result').value,
            createdSource: 'UI',
            lastUpdatedSource: 'UI'
        };
        try {
            await apiRequest(id ? `api/commands/${id}` : 'api/commands', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            $('command-modal').classList.remove('active');
            await loadCommands();
            showNotice(id ? 'Command updated successfully.' : 'Command created successfully.');
        } catch (err) {
            showNotice('Failed to save command: ' + err.message, true);
        }
    };

    const deleteCommand = async (id) => {
        if (!window.confirm('Are you sure you want to delete this command?')) return;
        try {
            await apiRequest(`api/commands/${id}`, { method: 'DELETE' });
            await loadCommands();
            showNotice('Command deleted successfully.');
        } catch (err) {
            showNotice('Failed to delete command: ' + err.message, true);
        }
    };

    // Alerts Management
    const loadAlertsData = async () => {
        try {
            state.alerts = await apiRequest('api/alerts', { silent: true }) || [];
            state.alertEvents = await apiRequest('api/alerts/events', { silent: true }) || [];
            state.unreadAlerts = await apiRequest('api/alerts/events/unread', { silent: true }) || [];
            updateNotificationBell();

            if (state.activeAlertTab === 'rules') renderAlertRules();
            else if (state.activeAlertTab === 'events') renderAlertEvents();
            else if (state.activeAlertTab === 'conditions') loadConditionTypes();
        } catch (err) {
            showNotice('Failed to load alert data: ' + err.message, true);
        }
    };

    const updateNotificationBell = () => {
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
                    await apiRequest(`api/alerts/events/${id}/read`, { method: 'PUT', silent: true });
                    await loadAlertsData();
                } catch (err) {
                    showNotice(err.message, true);
                }
            };
        });
    };

    $('notification-bell').onclick = (e) => {
        e.stopPropagation();
        $('notification-dropdown').classList.toggle('active');
    };

    document.addEventListener('click', (e) => {
        if (!e.target.closest('.notification-container')) {
            $('notification-dropdown')?.classList.remove('active');
        }
    });

    $('mark-all-read').onclick = async () => {
        try {
            await apiRequest('api/alerts/events/read-all', { method: 'PUT' });
            await loadAlertsData();
            showNotice('All alerts marked as read.');
        } catch (err) {
            showNotice('Failed to mark all as read: ' + err.message, true);
        }
    };

    const renderAlertRules = () => {
        const container = $('alert-rules-table');
        if (!container) return;

        if (!state.alerts.length) {
            container.innerHTML = '<div class="empty-state">No alert rules configured. Click "+ Add alert rule" to set up health rules.</div>';
            return;
        }

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
                            <button class="btn danger btn-sm" data-alert-delete="${a.id}">Delete</button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        container.innerHTML = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Rule Name</th>
                            <th>Target</th>
                            <th>Condition</th>
                            <th>State</th>
                            <th style="text-align: right;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;

        container.querySelectorAll('[data-alert-toggle]').forEach((b) => {
            b.onclick = async () => {
                const id = Number(b.dataset.alertToggle);
                try {
                    await apiRequest(`api/alerts/${id}/toggle`, { method: 'PUT' });
                    await loadAlertsData();
                } catch (err) {
                    showNotice(err.message, true);
                }
            };
        });

        container.querySelectorAll('[data-alert-edit]').forEach((b) => {
            b.onclick = () => openAlertModal(Number(b.dataset.alertEdit));
        });

        container.querySelectorAll('[data-alert-delete]').forEach((b) => {
            b.onclick = () => deleteAlertRule(Number(b.dataset.alertDelete));
        });
    };

    const renderAlertEvents = () => {
        const container = $('alert-events-table');
        if (!container) return;

        if (!state.alertEvents.length) {
            container.innerHTML = '<div class="empty-state">No triggered alert events recorded.</div>';
            return;
        }

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
        }).join('');

        container.innerHTML = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Alert ID</th>
                            <th>Target Scope</th>
                            <th>Message</th>
                            <th>Status</th>
                            <th>Timestamp</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;
    };

    const loadConditionTypes = async () => {
        try {
            state.conditionTypes = await apiRequest('api/alerts/condition-types', { silent: true }) || [];
            renderConditionTypes();
        } catch (err) {
            showNotice('Failed to load condition types: ' + err.message, true);
        }
    };

    const renderConditionTypes = () => {
        const container = $('condition-types-table');
        if (!container) return;

        if (!state.conditionTypes.length) {
            container.innerHTML = '<div class="empty-state">No condition types available.</div>';
            return;
        }

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
        `).join('');

        container.innerHTML = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Type Name</th>
                            <th>Description</th>
                            <th>Mappings</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;

        container.querySelectorAll('[data-view-mappings]').forEach((b) => {
            b.onclick = () => {
                const typeId = Number(b.dataset.viewMappings);
                const typeName = b.dataset.typeName;
                loadConditionMappings(typeId, typeName);
            };
        });
    };

    const loadConditionMappings = async (typeId, typeName) => {
        state.selectedConditionType = { id: typeId, name: typeName };
        try {
            state.conditionMappings = await apiRequest(`api/alerts/condition-types/${typeId}/mappings`, { silent: true }) || [];
            renderConditionMappings();
        } catch (err) {
            showNotice('Failed to load condition mappings: ' + err.message, true);
        }
    };

    const renderConditionMappings = () => {
        const container = $('condition-mappings-container');
        if (!container) return;

        container.style.display = 'block';
        $('selected-condition-type-title').textContent = `Mappings for: ${state.selectedConditionType.name}`;

        const listContainer = $('condition-mappings-list');
        if (!state.conditionMappings.length) {
            listContainer.innerHTML = '<div class="empty-state">No mappings for this type. Click "+ Add mapping" to add one.</div>';
            return;
        }

        const rows = state.conditionMappings.map((m) => `
            <tr>
                <td><strong>${escapeHtml(m.conditionKey)}</strong></td>
                <td><code>${escapeHtml(m.conditionValue)}</code></td>
                <td>${escapeHtml(m.description || '')}</td>
                <td>
                    <button class="btn danger btn-sm" data-delete-mapping="${m.id}">Delete</button>
                </td>
            </tr>
        `).join('');

        listContainer.innerHTML = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>Key</th>
                            <th>Value</th>
                            <th>Description</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;

        listContainer.querySelectorAll('[data-delete-mapping]').forEach((b) => {
            b.onclick = async () => {
                if (!window.confirm('Delete this mapping?')) return;
                const mappingId = Number(b.dataset.deleteMapping);
                try {
                    await apiRequest(`api/alerts/condition-mappings/${mappingId}`, { method: 'DELETE' });
                    await loadConditionMappings(state.selectedConditionType.id, state.selectedConditionType.name);
                    showNotice('Mapping deleted.');
                } catch (err) {
                    showNotice(err.message, true);
                }
            };
        });
    };

    document.querySelectorAll('.tab-btn[data-alert-tab]').forEach((btn) => {
        btn.onclick = () => {
            document.querySelectorAll('.tab-btn[data-alert-tab]').forEach((b) => b.classList.remove('active'));
            btn.classList.add('active');
            state.activeAlertTab = btn.dataset.alertTab;

            $('alert-tab-rules').style.display = state.activeAlertTab === 'rules' ? 'block' : 'none';
            $('alert-tab-events').style.display = state.activeAlertTab === 'events' ? 'block' : 'none';
            $('alert-tab-conditions').style.display = state.activeAlertTab === 'conditions' ? 'block' : 'none';

            loadAlertsData();
        };
    });

    const openAlertModal = (id = null) => {
        $('alert-form').reset();
        $('alert-id').value = id || '';
        $('alert-enabled').checked = true;

        if (id) {
            const alert = state.alerts.find((a) => a.id === id);
            if (!alert) return showNotice('Alert rule not found.', true);
            $('alert-name').value = alert.name || '';
            $('alert-target-type').value = alert.targetType || 'SERVER';
            $('alert-server').value = alert.serverId || '';
            $('alert-service').value = alert.serviceId || '';
            $('alert-condition-type').value = alert.conditionType || 'SERVICE_STATUS';
            $('alert-operator').value = alert.operator || 'EQUALS';
            $('alert-condition-value').value = alert.conditionValue || '';
            $('alert-enabled').checked = Boolean(alert.enabled);
        }

        toggleAlertTargetFields();
        $('alert-modal').classList.add('active');
    };

    const toggleAlertTargetFields = () => {
        const isServer = $('alert-target-type').value === 'SERVER';
        $('alert-server-group').style.display = isServer ? 'block' : 'none';
        $('alert-service-group').style.display = !isServer ? 'block' : 'none';
    };

    $('alert-target-type').onchange = toggleAlertTargetFields;

    $('alert-form').onsubmit = async (e) => {
        e.preventDefault();
        const id = $('alert-id').value;
        const isServer = $('alert-target-type').value === 'SERVER';

        const payload = {
            name: $('alert-name').value.trim(),
            targetType: $('alert-target-type').value,
            serverId: isServer ? Number($('alert-server').value) : null,
            serviceId: !isServer ? Number($('alert-service').value) : null,
            conditionType: $('alert-condition-type').value,
            operator: $('alert-operator').value,
            conditionValue: $('alert-condition-value').value.trim(),
            enabled: $('alert-enabled').checked
        };

        try {
            await apiRequest(id ? `api/alerts/${id}` : 'api/alerts', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            $('alert-modal').classList.remove('active');
            await loadAlertsData();
            showNotice(id ? 'Alert rule updated.' : 'Alert rule created.');
        } catch (err) {
            showNotice('Failed to save alert rule: ' + err.message, true);
        }
    };

    const deleteAlertRule = async (id) => {
        if (!window.confirm('Delete this alert rule?')) return;
        try {
            await apiRequest(`api/alerts/${id}`, { method: 'DELETE' });
            await loadAlertsData();
            showNotice('Alert rule deleted.');
        } catch (err) {
            showNotice('Failed to delete alert: ' + err.message, true);
        }
    };

    // Log Event Modal
    $('open-log-event-modal')?.addEventListener('click', () => {
        $('log-event-form').reset();
        $('log-event-modal').classList.add('active');
    });

    $('log-event-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            alertId: Number($('event-log-alert-id').value),
            serverId: $('event-log-server').value ? Number($('event-log-server').value) : null,
            serviceId: $('event-log-service-id').value ? Number($('event-log-service-id').value) : null,
            triggerCondition: $('event-log-condition').value.trim(),
            alertStatus: $('event-log-status').value,
            alertMessage: $('event-log-message').value.trim()
        };

        try {
            await apiRequest('api/alerts/events/log', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            $('log-event-modal').classList.remove('active');
            await loadAlertsData();
            showNotice('Alert event logged successfully.');
        } catch (err) {
            showNotice('Failed to log event: ' + err.message, true);
        }
    });

    // Add Mapping Modal
    $('add-condition-mapping-btn')?.addEventListener('click', () => {
        if (!state.selectedConditionType) return;
        $('mapping-form').reset();
        $('mapping-modal').classList.add('active');
    });

    $('mapping-form')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const payload = {
            conditionKey: $('mapping-key').value.trim(),
            conditionValue: $('mapping-value').value.trim(),
            description: $('mapping-desc').value.trim()
        };

        try {
            await apiRequest(`api/alerts/condition-types/${state.selectedConditionType.id}/mappings`, {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            $('mapping-modal').classList.remove('active');
            await loadConditionMappings(state.selectedConditionType.id, state.selectedConditionType.name);
            showNotice('Condition mapping created.');
        } catch (err) {
            showNotice('Failed to create mapping: ' + err.message, true);
        }
    });

    // System Logs Viewer & Live Stream
    const initSystemLogs = async () => {
        try {
            const res = await apiRequest('api/system/logs?lines=150', { silent: true });
            state.systemLogs = unwrap(res) || [];
            renderSystemLogs();
            connectSystemLogsStream();
        } catch (err) {
            const terminal = $('system-logs-body');
            if (terminal) terminal.textContent = 'Failed to load system logs: ' + err.message;
        }
    };

    const renderSystemLogs = () => {
        const terminal = $('system-logs-body');
        if (!terminal) return;

        const filtered = state.systemLogs.filter((entry) => {
            if (state.logMinLevel === 'ALL') return true;
            return entry.level === state.logMinLevel;
        });

        if (!filtered.length) {
            terminal.textContent = 'No log entries matching criteria.';
            return;
        }

        terminal.textContent = filtered.map((entry) => {
            return `[${entry.timestamp || ''}] [${entry.level || 'INFO'}] [${entry.logger || ''}] ${entry.message || ''}`;
        }).join('\n');

        if (state.autoScrollLogs) {
            terminal.scrollTop = terminal.scrollHeight;
        }
    };

    const connectSystemLogsStream = () => {
        if (systemLogStreamSource) return;

        try {
            systemLogStreamSource = new EventSource(apiUrl('api/system/logs/stream'));
            systemLogStreamSource.onmessage = (event) => {
                if (state.isLogStreamPaused) return;
                try {
                    const entry = JSON.parse(event.data);
                    state.systemLogs.push(entry);
                    if (state.systemLogs.length > 500) state.systemLogs.shift();
                    renderSystemLogs();
                } catch {
                    // Raw string
                }
            };
            systemLogStreamSource.onerror = () => {
                // Stream reconnects automatically
            };
        } catch {
            // Ignore if SSE unsupported
        }
    };

    $('log-level-filter')?.addEventListener('change', (e) => {
        state.logMinLevel = e.target.value;
        renderSystemLogs();
    });

    $('log-autoscroll-toggle')?.addEventListener('change', (e) => {
        state.autoScrollLogs = e.target.checked;
    });

    $('clear-system-logs-btn')?.addEventListener('click', () => {
        state.systemLogs = [];
        renderSystemLogs();
    });

    $('pause-system-logs-btn')?.addEventListener('click', (e) => {
        state.isLogStreamPaused = !state.isLogStreamPaused;
        e.target.textContent = state.isLogStreamPaused ? 'Resume Stream' : 'Pause Stream';
        e.target.className = state.isLogStreamPaused ? 'btn success btn-sm' : 'btn secondary btn-sm';
    });

    // Settings & Runtime Configuration
    const loadSettings = async () => {
        try {
            state.paths = unwrap(await apiRequest('api/paths', { silent: true })) || [];
            state.scripts = unwrap(await apiRequest('api/scripts', { silent: true })) || [];
            const runtime = unwrap(await apiRequest('api/configuration/runtime', { silent: true }));

            if ($('healthcheck-enabled')) $('healthcheck-enabled').checked = Boolean(runtime.healthCheckEnabled);
            if ($('service-management-enabled')) $('service-management-enabled').checked = Boolean(runtime.serviceManagementEnabled);
            if ($('alert-management-enabled')) $('alert-management-enabled').checked = Boolean(runtime.alertManagementEnabled);
            if ($('health-status-population-enabled')) $('health-status-population-enabled').checked = Boolean(runtime.healthStatusPopulationEnabled);
            if ($('healthcheck-cron')) $('healthcheck-cron').value = runtime.healthCheckCron || '';
            if ($('runtime-config-status')) $('runtime-config-status').textContent = 'Configuration Loaded';

            renderPaths();
            renderScripts();
        } catch (err) {
            if ($('runtime-config-status')) $('runtime-config-status').textContent = 'Unavailable';
            showNotice('Failed to load settings: ' + err.message, true);
        }
    };

    $('save-runtime-config')?.addEventListener('click', async () => {
        try {
            const runtime = unwrap(await apiRequest('api/configuration/runtime', {
                method: 'PUT',
                body: JSON.stringify({
                    healthCheckEnabled: $('healthcheck-enabled').checked,
                    serviceManagementEnabled: $('service-management-enabled').checked,
                    alertManagementEnabled: $('alert-management-enabled').checked,
                    healthStatusPopulationEnabled: $('health-status-population-enabled').checked
                })
            }));
            $('healthcheck-enabled').checked = Boolean(runtime.healthCheckEnabled);
            $('service-management-enabled').checked = Boolean(runtime.serviceManagementEnabled);
            $('alert-management-enabled').checked = Boolean(runtime.alertManagementEnabled);
            $('health-status-population-enabled').checked = Boolean(runtime.healthStatusPopulationEnabled);
            if ($('runtime-config-status')) $('runtime-config-status').textContent = 'Saved';
            showNotice('Runtime configuration saved successfully.');
        } catch (err) {
            showNotice('Failed to save runtime configuration: ' + err.message, true);
        }
    });

    const renderPaths = () => {
        const container = $('path-table');
        if (!container) return;

        if (!state.paths.length) {
            container.innerHTML = '<div class="empty-state">No paths configured.</div>';
            return;
        }

        const rows = state.paths.map((p) => `
            <tr>
                <td><strong>#${p.id}</strong></td>
                <td><strong>${escapeHtml(p.pathName)}</strong></td>
                <td><code>${escapeHtml(p.pathDescription || '')}</code></td>
                <td>
                    <div class="service-actions">
                        <button class="btn secondary btn-sm" data-path-edit="${p.id}">Edit</button>
                        <button class="btn danger btn-sm" data-path-delete="${p.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Path Name</th>
                            <th>Location / Description</th>
                            <th style="text-align: right;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;

        container.querySelectorAll('[data-path-edit]').forEach((b) => {
            b.onclick = () => openPathModal(Number(b.dataset.pathEdit));
        });
        container.querySelectorAll('[data-path-delete]').forEach((b) => {
            b.onclick = () => deletePath(Number(b.dataset.pathDelete));
        });
    };

    const renderScripts = () => {
        const container = $('script-table');
        if (!container) return;

        if (!state.scripts.length) {
            container.innerHTML = '<div class="empty-state">No scripts configured.</div>';
            return;
        }

        const rows = state.scripts.map((s) => `
            <tr>
                <td><strong>#${s.id}</strong></td>
                <td><strong>${escapeHtml(s.scriptName)}</strong></td>
                <td><span class="status info">${escapeHtml(s.scriptExtension || 'SH')}</span></td>
                <td>
                    <div class="service-actions">
                        <button class="btn secondary btn-sm" data-script-edit="${s.id}">Edit</button>
                        <button class="btn danger btn-sm" data-script-delete="${s.id}">Delete</button>
                    </div>
                </td>
            </tr>
        `).join('');

        container.innerHTML = `
            <div class="table-container">
                <table>
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Script Name</th>
                            <th>Extension</th>
                            <th style="text-align: right;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;

        container.querySelectorAll('[data-script-edit]').forEach((b) => {
            b.onclick = () => openScriptModal(Number(b.dataset.scriptEdit));
        });
        container.querySelectorAll('[data-script-delete]').forEach((b) => {
            b.onclick = () => deleteScript(Number(b.dataset.scriptDelete));
        });
    };

    const openPathModal = (id = null) => {
        $('path-crud-form').reset();
        $('path-id').value = id || '';
        renderServerSelectDropdowns();
        if (id) {
            const p = state.paths.find((item) => item.id === id);
            if (!p) return showNotice('Path not found.', true);
            $('path-name').value = p.pathName || '';
            $('path-description').value = p.pathDescription || '';
        } else if (state.selectedServer) {
            $('path-server').value = state.selectedServer.id;
        }
        $('path-modal').classList.add('active');
    };

    $('path-crud-form').onsubmit = async (e) => {
        e.preventDefault();
        const serverId = Number($('path-server').value);
        const server = state.servers.find((s) => s.id === serverId);
        if (!server) return showNotice('Please select a server.', true);

        const id = $('path-id').value;
        const payload = {
            id: id ? Number(id) : null,
            pathName: $('path-name').value.trim(),
            pathDescription: $('path-description').value.trim(),
            servers: [server],
            scripts: [],
            createdSource: 'UI',
            lastUpdatedSource: 'UI'
        };

        try {
            await apiRequest(id ? `api/paths/${id}` : 'api/paths', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            $('path-modal').classList.remove('active');
            await loadSettings();
            showNotice('Path saved successfully.');
        } catch (err) {
            showNotice('Failed to save path: ' + err.message, true);
        }
    };

    const deletePath = async (id) => {
        if (!window.confirm('Delete this path?')) return;
        try {
            await apiRequest(`api/paths/${id}`, { method: 'DELETE' });
            await loadSettings();
            showNotice('Path deleted.');
        } catch (err) {
            showNotice('Failed to delete path: ' + err.message, true);
        }
    };

    const openScriptModal = (id = null) => {
        $('script-crud-form').reset();
        $('script-id').value = id || '';
        if (id) {
            const s = state.scripts.find((item) => item.id === id);
            if (!s) return showNotice('Script not found.', true);
            $('script-name').value = s.scriptName || '';
            $('script-extension').value = s.scriptExtension || 'SH';
        }
        $('script-modal').classList.add('active');
    };

    $('script-crud-form').onsubmit = async (e) => {
        e.preventDefault();
        const id = $('script-id').value;
        const payload = {
            id: id ? Number(id) : null,
            scriptName: $('script-name').value.trim(),
            scriptExtension: $('script-extension').value.trim(),
            paths: [],
            createdSource: 'UI',
            lastUpdatedSource: 'UI'
        };

        try {
            await apiRequest(id ? `api/scripts/${id}` : 'api/scripts', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            $('script-modal').classList.remove('active');
            await loadSettings();
            showNotice('Script saved successfully.');
        } catch (err) {
            showNotice('Failed to save script: ' + err.message, true);
        }
    };

    const deleteScript = async (id) => {
        if (!window.confirm('Delete this script?')) return;
        try {
            await apiRequest(`api/scripts/${id}`, { method: 'DELETE' });
            await loadSettings();
            showNotice('Script deleted.');
        } catch (err) {
            showNotice('Failed to delete script: ' + err.message, true);
        }
    };

    // Global Event Listeners & Buttons
    $('server-select').onchange = (e) => selectServer(Number(e.target.value));
    $('refresh-services')?.addEventListener('click', loadServices);
    $('start-all-services')?.addEventListener('click', () => bulkServiceAction('start'));
    $('stop-all-services')?.addEventListener('click', () => bulkServiceAction('stop'));
    $('restart-all-services')?.addEventListener('click', () => bulkServiceAction('restart'));

    $('add-server')?.addEventListener('click', () => openServerModal());
    $('add-server-page')?.addEventListener('click', () => openServerModal());
    $('add-service')?.addEventListener('click', () => openServiceModal());
    $('add-service-page')?.addEventListener('click', () => openServiceModal());
    $('add-command')?.addEventListener('click', () => openCommandModal());
    $('refresh-commands')?.addEventListener('click', loadCommands);
    $('add-alert')?.addEventListener('click', () => openAlertModal());
    $('refresh-alerts')?.addEventListener('click', loadAlertsData);
    $('add-path')?.addEventListener('click', () => openPathModal());
    $('add-script')?.addEventListener('click', () => openScriptModal());

    $('service-filter-server')?.addEventListener('change', loadAllServicesView);

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

    // Close Modals
    document.querySelectorAll('[data-close]').forEach((btn) => {
        btn.onclick = () => {
            const modalId = btn.dataset.close;
            if (modalId === 'logs-modal' && logStreamSource) {
                logStreamSource.close();
                logStreamSource = null;
            }
            const modal = $(modalId);
            if (modal) modal.classList.remove('active');
        };
    });

    // Sidebar navigation buttons
    document.querySelectorAll('.nav button').forEach((btn) => {
        btn.onclick = () => showPage(btn.dataset.page);
    });

    // Startup Initialization
    const init = async () => {
        initHashRouting();
        await loadServers();
        await loadCommands();
        await loadAlertsData();
    };

    init();
})();
