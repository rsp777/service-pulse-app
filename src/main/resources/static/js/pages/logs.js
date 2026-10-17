/**
 * logs.js — System Logs Viewer and Live SSE Stream module.
 *
 * Responsibilities:
 *  - Fetch recent system log entries
 *  - Establish and manage SSE live log stream
 *  - Filter logs by level (ALL, INFO, WARN, ERROR, DEBUG)
 *  - Support auto-scroll, clear, and pause/resume stream
 *
 * Depends on: config.js, api.js, ui.js, state.js
 */

'use strict';

// ── Data Loading & Stream ─────────────────────────────────────────────────

async function initSystemLogs() {
    try {
        const res = await apiRequest(API.systemLogs.recent(150), { silent: true });
        state.systemLogs = unwrap(res) || [];
        renderSystemLogs();
        connectSystemLogsStream();
    } catch (err) {
        const terminal = $('system-logs-body');
        if (terminal) terminal.textContent = 'Failed to load system logs: ' + err.message;
    }
}

function renderSystemLogs() {
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

    terminal.textContent = filtered.map((entry) =>
        `[${entry.timestamp || ''}] [${entry.level || 'INFO'}] [${entry.logger || ''}] ${entry.message || ''}`
    ).join('\n');

    if (state.autoScrollLogs) {
        terminal.scrollTop = terminal.scrollHeight;
    }
}

function connectSystemLogsStream() {
    if (systemLogStreamSource) return;

    try {
        const handleLogEvent = (event) => {
            if (state.isLogStreamPaused) return;
            try {
                const entry = typeof event.data === 'string' && event.data.startsWith('{')
                    ? JSON.parse(event.data)
                    : { timestamp: new Date().toLocaleTimeString(), level: 'INFO', logger: 'app', message: event.data };
                state.systemLogs.push(entry);
                if (state.systemLogs.length > 500) state.systemLogs.shift();
                renderSystemLogs();
            } catch {
                // Ignore unparseable frames
            }
        };

        systemLogStreamSource.addEventListener('log', handleLogEvent);
        systemLogStreamSource.onmessage = handleLogEvent;
        systemLogStreamSource.onerror = () => {
            // Stream reconnects automatically
        };
    } catch {
        // Ignore if SSE unsupported
    }
}

// ── Event Handlers ─────────────────────────────────────────────────────────

function initLogsPage() {
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
        e.target.className   = state.isLogStreamPaused ? 'btn success btn-sm' : 'btn secondary btn-sm';
    });
}
