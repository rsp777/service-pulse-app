package com.pawar.todo.amt.logging;

/**
 * Immutable snapshot of a single log event stored in the in-memory ring buffer.
 */
public record LogEntry(
        String timestamp,
        String level,
        String logger,
        String thread,
        String message) {
}
