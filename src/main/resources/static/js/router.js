/**
 * router.js — Hash-based client-side routing.
 *
 * Maps URL hash fragments (#dashboard, #servers, …) to page sections.
 * Calls the per-page load function when a page is activated.
 *
 * Depends on: config.js (PAGES, DEFAULT_PAGE), ui.js ($), state.js
 * Called by: main.js (initHashRouting)
 */

'use strict';

/**
 * Show one page section, hide all others, and highlight the active nav button.
 * Fires the per-page data-load function on every navigation.
 *
 * @param {string} pageName - One of PAGES
 */
function showPage(pageName) {
    state.activePage = pageName;

    document.querySelectorAll('.page').forEach((el) =>
        el.classList.toggle('active', el.id === pageName)
    );
    document.querySelectorAll('.nav button').forEach((btn) =>
        btn.classList.toggle('active', btn.dataset.page === pageName)
    );

    // Keep hash in sync without triggering another hashchange
    if (window.location.hash.replace(/^#/, '') !== pageName) {
        window.location.hash = pageName;
    }

    // Trigger per-page data loads
    const loaders = {
        dashboard: () => { updateDashboardStats(); if (state.selectedServer) loadServices(); },
        servers:   renderServersTable,
        services:  loadAllServicesView,
        commands:  loadCommands,
        alerts:    loadAlertsData,
        logs:      initSystemLogs,
        settings:  loadSettings,
    };
    loaders[pageName]?.();
}

/**
 * Bootstrap hash routing: listen for hashchange and handle initial hash.
 * Call once on app startup.
 */
function initHashRouting() {
    const handleHash = () => {
        const hash = window.location.hash.replace(/^#/, '');
        showPage(PAGES.includes(hash) ? hash : DEFAULT_PAGE);
    };
    window.addEventListener('hashchange', handleHash);
    handleHash();
}

/** Wire all sidebar nav buttons. */
function initNavigation() {
    document.querySelectorAll('.nav button').forEach((btn) => {
        btn.onclick = () => showPage(btn.dataset.page);
    });
}
