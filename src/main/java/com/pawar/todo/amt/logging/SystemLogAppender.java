package com.pawar.todo.amt.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A thread-safe, fixed-capacity circular log appender that stores the most
 * recent log events in memory so the SystemLogsController can serve them
 * without reading from disk.
 *
 * Registered as the "MEMORY" appender in logback.xml.
 */
public class SystemLogAppender extends AppenderBase<ILoggingEvent> {

    /** Maximum number of log entries held in memory at any time. */
    private static final int CAPACITY = 2000;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    .withZone(ZoneId.systemDefault());

    /** Shared ring buffer – access must be synchronised on BUFFER. */
    private static final Deque<LogEntry> BUFFER = new ArrayDeque<>(CAPACITY);

    // ── AppenderBase ──────────────────────────────────────────────────────────

    @Override
    protected void append(ILoggingEvent event) {
        String timestamp = FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()));
        String level     = event.getLevel().toString();
        String logger    = abbreviate(event.getLoggerName(), 40);
        String thread    = event.getThreadName();
        String message   = event.getFormattedMessage();

        // Include exception info if present
        if (event.getThrowableProxy() != null) {
            message = message + " | " + event.getThrowableProxy().getClassName()
                    + ": " + event.getThrowableProxy().getMessage();
        }

        LogEntry entry = new LogEntry(timestamp, level, logger, thread, message);

        synchronized (BUFFER) {
            if (BUFFER.size() >= CAPACITY) {
                BUFFER.pollFirst();          // discard oldest
            }
            BUFFER.addLast(entry);
        }
    }

    // ── Static API for SystemLogsController ──────────────────────────────────

    /**
     * Returns up to {@code lines} most recent entries, optionally filtered
     * to a minimum level (INFO, WARN, ERROR).
     */
    public static List<LogEntry> getRecent(int lines, String minLevel) {
        List<LogEntry> snapshot;
        synchronized (BUFFER) {
            snapshot = new ArrayList<>(BUFFER);
        }

        int minOrdinal = levelOrdinal(minLevel);
        List<LogEntry> filtered = new ArrayList<>();
        for (LogEntry e : snapshot) {
            if (levelOrdinal(e.level()) >= minOrdinal) {
                filtered.add(e);
            }
        }

        // Return only the last `lines` entries
        int from = Math.max(0, filtered.size() - lines);
        return filtered.subList(from, filtered.size());
    }

    /**
     * Returns all entries added after the entry with the given sequential
     * position (used for polling by the SSE stream).
     *
     * @param afterIndex 0-based index into the snapshot taken at call time;
     *                   pass -1 to get all entries.
     */
    public static List<LogEntry> getSince(int afterIndex) {
        List<LogEntry> snapshot;
        synchronized (BUFFER) {
            snapshot = new ArrayList<>(BUFFER);
        }
        if (afterIndex < 0 || afterIndex >= snapshot.size()) {
            return snapshot;
        }
        return snapshot.subList(afterIndex, snapshot.size());
    }

    /** Returns total number of entries currently in the buffer. */
    public static int bufferSize() {
        synchronized (BUFFER) {
            return BUFFER.size();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int levelOrdinal(String level) {
        return switch (level == null ? "ALL" : level.toUpperCase()) {
            case "TRACE" -> 0;
            case "DEBUG" -> 1;
            case "INFO"  -> 2;
            case "WARN"  -> 3;
            case "ERROR" -> 4;
            case "OFF"   -> 5;
            default      -> 0; // ALL
        };
    }

    /** Shortens a fully-qualified class name to at most {@code max} chars. */
    private static String abbreviate(String name, int max) {
        if (name == null || name.length() <= max) return name;
        // Keep last segment in full, abbreviate leading segments
        String[] parts = name.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            sb.append(parts[i].charAt(0)).append('.');
        }
        sb.append(parts[parts.length - 1]);
        String abbreviated = sb.toString();
        return abbreviated.length() <= max ? abbreviated : abbreviated.substring(abbreviated.length() - max);
    }
}
