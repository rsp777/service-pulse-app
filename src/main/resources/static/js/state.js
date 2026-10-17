/**
 * state.js — Single mutable application state store.
 *
 * All runtime data lives here. Page modules read from and write to `state`
 * directly — no prop drilling, no duplicated copies (DRY).
 *
 * Immutable shape: add new fields here when needed; never declare local
 * state inside page modules.
 */

'use strict';

const state = {
    // Domain data
    servers:                    [],
    selectedServer:             null,
    commands:                   [],
    paths:                      [],
    scripts:                    [],
    alerts:                     [],
    alertEvents:                [],
    unreadAlerts:               [],
    conditionTypes:             [],
    selectedConditionType:      null,
    conditionMappings:          [],
    healthStatuses:             {},
    serverServiceConfigurations: [],
    systemLogs:                 [],

    // UI state
    activePage:         'dashboard',
    activeAlertTab:     'rules',
    isLogStreamPaused:  false,
    autoScrollLogs:     true,
    logMinLevel:        'ALL',
};

// ── SSE stream handles (module-level, not page-level) ──────────────────────
let logStreamSource         = null;
let serviceActionStreamSource = null;
let systemLogStreamSource   = null;
