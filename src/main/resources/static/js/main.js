/**
 * main.js — Application entrypoint and orchestrator.
 *
 * Responsibilities:
 *  - Bootstrap all page modules and event listeners
 *  - Global modal backdrop and ESC keyboard dismissal
 *  - Initial data load orchestration
 *
 * Execution order:
 *  1. config.js
 *  2. state.js
 *  3. ui.js
 *  4. api.js
 *  5. router.js
 *  6. pages/servers.js
 *  7. pages/dashboard.js
 *  8. pages/services.js
 *  9. pages/commands.js
 *  10. pages/alerts.js
 *  11. pages/logs.js
 *  12. pages/settings.js
 *  13. main.js (this file)
 */

'use strict';

// ── Global Modal Behavior ─────────────────────────────────────────────────
// Issue #31: Ensure closing logs modal terminates stream and cleans up resources [Author: Ravindra Singh Pawar / Antigravity]

function initModalDismissal() {
    // Dismiss via data-close button
    document.querySelectorAll('[data-close]').forEach((btn) => {
        btn.onclick = () => {
            const modalId = btn.dataset.close;
            if (modalId === 'logs-modal') {
                if (typeof closeServiceLogsModal === 'function') {
                    closeServiceLogsModal();
                } else if (logStreamSource) {
                    logStreamSource.close();
                    logStreamSource = null;
                }
            }
            const modal = $(modalId);
            if (modal) modal.classList.remove('active');
        };
    });

    // Dismiss via ESC key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal.active').forEach((modal) => {
                if (modal.id === 'logs-modal') {
                    if (typeof closeServiceLogsModal === 'function') {
                        closeServiceLogsModal();
                    } else if (logStreamSource) {
                        logStreamSource.close();
                        logStreamSource = null;
                    }
                }
                modal.classList.remove('active');
            });
            $('notification-dropdown')?.classList.remove('active');
        }
    });

    // Dismiss via clicking on modal backdrop (outside .modal-content)
    document.querySelectorAll('.modal').forEach((modal) => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                if (modal.id === 'logs-modal') {
                    if (typeof closeServiceLogsModal === 'function') {
                        closeServiceLogsModal();
                    } else if (logStreamSource) {
                        logStreamSource.close();
                        logStreamSource = null;
                    }
                }
                modal.classList.remove('active');
            }
        });
    });
}

// ── Application Bootstrap ──────────────────────────────────────────────────

async function bootstrapApp() {
    // 1. Navigation & Modals
    initModalDismissal();
    initNavigation();

    // 2. Page Event Listeners
    initServerFormHandlers();
    initServicesPage();
    initCommandsPage();
    initAlertsPage();
    initLogsPage();
    initSettingsPage();
    initCommandExecutionPanel();
    initTerminalControls();
    if (typeof initServiceLogsModalControls === 'function') {
        initServiceLogsModalControls();
    }
    initBulkServiceButtons();

    // 3. Routing
    initHashRouting();

    // 4. Initial Core Data Fetch
    await loadServers();
    await loadCommands();
    await loadAlertsData();
}

// Kick off when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bootstrapApp);
} else {
    bootstrapApp();
}
