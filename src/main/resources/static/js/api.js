/**
 * api.js — HTTP / fetch abstraction layer.
 *
 * All network communication is routed through these two functions.
 * Page modules must never call `fetch()` directly — use `apiRequest()`.
 *
 * Responsibilities:
 *  - Resolve URLs via config.js (no raw strings)
 *  - Inject Content-Type header
 *  - Decode and surface structured error messages
 *  - Expose `unwrap()` to peel the data envelope off ApiResponse<T>
 */

'use strict';

/**
 * Unwrap an ApiResponse<T> envelope returned by the Spring backend.
 * Falls back to the raw value if it is not an envelope.
 * @param {*} val
 * @returns {*}
 */
function unwrap(val) {
    return val?.data ?? val;
}

/**
 * Make an authenticated JSON API request.
 *
 * @param {string} endpoint - Relative path from config.js, e.g. API.servers.list
 * @param {RequestInit & { silent?: boolean }} [options]
 * @returns {Promise<*>} Parsed JSON body, or null for 204 No Content
 * @throws {Error} With `.message` set from the backend error body when available
 */
async function apiRequest(endpoint, options = {}) {
    const url     = apiUrl(endpoint);
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    const response = await fetch(url, { ...options, headers });

    if (!response.ok) {
        const body = await response.text();
        try {
            const payload = JSON.parse(body);
            const err = new Error(payload.message || `${response.status} Request Failed`);
            err.code = payload.code;
            throw err;
        } catch (inner) {
            if (inner instanceof SyntaxError) {
                throw new Error(body || `${response.status} Request Failed`);
            }
            throw inner;
        }
    }

    if (response.status === 204) return null;

    const payload = await response.json();

    // Surface success/error notices unless caller suppresses them
    if (payload?.message && !options.silent) {
        showNotice(payload.message, payload.success === false);
    }

    return payload;
}
