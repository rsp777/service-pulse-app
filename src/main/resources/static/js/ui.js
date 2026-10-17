/**
 * ui.js — Shared UI utilities.
 *
 * Functions here are used by every page module.
 * Only pure presentation helpers belong here — no API calls, no state writes.
 *
 * Utilities:
 *  - $()          element lookup by ID
 *  - escapeHtml() XSS-safe string escaping
 *  - showNotice() transient status bar notification
 *  - renderTable() DRY table renderer shared by all page modules
 */

'use strict';

/** Shorthand getElementById. */
const $ = (id) => document.getElementById(id);

/** HTML-escape a string to prevent XSS in innerHTML templates. */
function escapeHtml(str) {
    return String(str ?? '').replace(/[&<>"']/g, (ch) => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;',
    })[ch]);
}

/**
 * Show a transient notice bar message, auto-hidden after 4.5 s.
 * @param {string}  text
 * @param {boolean} [isError=false]
 */
function showNotice(text, isError = false) {
    const el = $('notice');
    if (!el) return;
    el.textContent = text;
    el.className   = 'visible' + (isError ? ' error' : '');
    clearTimeout(el._timer);
    el._timer = setTimeout(() => {
        el.className   = '';
        el.textContent = '';
    }, 4500);
}

/**
 * Render a data table into a container element.
 *
 * Eliminates ~10 near-identical table-render functions across page modules (DRY).
 *
 * @param {HTMLElement}   container   - Target DOM element
 * @param {string[]}      headers     - Column heading labels
 * @param {string[]}      rows        - Pre-rendered <tr>...</tr> HTML strings
 * @param {string}        [emptyMsg]  - Message shown when rows is empty
 * @param {Record<string, ()=>void>} [delegates] - Click delegate { '[data-attr]': handler(el) }
 */
function renderTable(container, headers, rows, emptyMsg = 'No data available.', delegates = {}) {
    if (!container) return;

    if (!rows.length) {
        container.innerHTML = `<div class="empty-state">${escapeHtml(emptyMsg)}</div>`;
        return;
    }

    const thHtml = headers.map((h, i) =>
        `<th${i === headers.length - 1 ? ' style="text-align:right"' : ''}>${escapeHtml(h)}</th>`
    ).join('');

    container.innerHTML = `
        <div class="table-container">
            <table>
                <thead><tr>${thHtml}</tr></thead>
                <tbody>${rows.join('')}</tbody>
            </table>
        </div>`;

    // Wire click delegates  { '[data-foo]': (el) => ... }
    Object.entries(delegates).forEach(([selector, handler]) => {
        container.querySelectorAll(selector).forEach((el) => {
            el.onclick = () => handler(el);
        });
    });
}
