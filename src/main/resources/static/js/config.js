/**
 * config.js — Centralized API URL registry.
 *
 * All backend endpoint paths are defined here as a single source of truth.
 * No raw strings like "api/servers" should appear anywhere else in the
 * JavaScript codebase — always reference this module instead (DRY, KISS).
 *
 * The base URL is resolved dynamically from the <base href="..."> element
 * so it works under any servlet context-path without configuration changes.
 */

'use strict';

/** Resolve base URL from the HTML <base> element (handles context-path). */
function resolveBaseUrl() {
    const base = document.querySelector('base');
    return base ? base.getAttribute('href').replace(/\/+$/, '') + '/' : '/';
}

const BASE_URL = resolveBaseUrl();

/**
 * Build a fully-qualified API URL from a relative path.
 * @param {string} path - Relative path, e.g. "api/servers" or "api/servers/1"
 * @returns {string}
 */
function apiUrl(path) {
    return BASE_URL + path.replace(/^\/+/, '');
}

/** All known API endpoint paths, grouped by domain. */
const API = {
    servers: {
        list:           'api/servers',
        byId:           (id) => `api/servers/${id}`,
        // Issue #26: Test SSH connection endpoints [Author: Ravindra Singh Pawar / Antigravity]
        testConnection: 'api/servers/test-connection',
        testServer:     (id) => `api/servers/${id}/test-connection`,
    },
    services: {
        list:         'api/services',
        byId:         (id)         => `api/services/${id}`,
        healthStatus: (serverId)   => `api/service-health-status/server/${serverId}`,
        execute:      (action, serverId, serviceId) =>
                        `api/manage-services/${action}-service?serverId=${serverId}&serviceId=${serviceId}`,
        bulkStream:   (action, serverId) =>
                        `api/manage-services/${action}-all-service/stream?serverId=${serverId}`,
        logStream:    (serverId, serviceId) =>
                        `api/logs/stream?serverId=${serverId}&serviceId=${serviceId}`,
        serverServiceConfigs: (serverId) =>
                        `api/server-service-configurations/server/${serverId}`,
        // Issue #27: All server configs and runtime status endpoints [Author: Ravindra Singh Pawar / Antigravity]
        serverServiceConfigsAll: 'api/server-service-configurations',
        runtimeStatus:           'api/services/runtime-status',
    },
    commands: {
        list:         'api/commands',
        byId:         (id)         => `api/commands/${id}`,
        execute:      (serverId, command) =>
                        `api/commands/execute?serverId=${serverId}&command=${encodeURIComponent(command)}`,
    },
    alerts: {
        list:         'api/alerts',
        byId:         (id)         => `api/alerts/${id}`,
        toggle:       (id)         => `api/alerts/${id}/toggle`,
        events:       {
            list:     'api/alerts/events',
            unread:   'api/alerts/events/unread',
            markRead: (id)         => `api/alerts/events/${id}/read`,
            markAll:  'api/alerts/events/read-all',
            log:      'api/alerts/events/log',
        },
        conditionTypes: {
            list:     'api/alerts/condition-types',
            mappings: (typeId)     => `api/alerts/condition-types/${typeId}/mappings`,
        },
        conditionMappings: {
            byId:     (id)         => `api/alerts/condition-mappings/${id}`,
        },
    },
    paths: {
        list:         'api/paths',
        byId:         (id)         => `api/paths/${id}`,
    },
    scripts: {
        list:         'api/scripts',
        byId:         (id)         => `api/scripts/${id}`,
    },
    config: {
        runtime:      'api/configuration/runtime',
    },
    systemLogs: {
        recent:       (lines = 150) => `api/system/logs?lines=${lines}`,
        stream:       'api/system/logs/stream',
    },
};

/** Valid SPA page identifiers (used by the router). */
const PAGES = ['dashboard', 'servers', 'services', 'commands', 'alerts', 'logs', 'settings'];

/** Default page shown when no hash is present or hash is invalid. */
const DEFAULT_PAGE = 'dashboard';
