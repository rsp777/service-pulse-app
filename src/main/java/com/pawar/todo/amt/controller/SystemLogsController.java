package com.pawar.todo.amt.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.pawar.todo.amt.logging.LogEntry;
import com.pawar.todo.amt.logging.SystemLogAppender;
import com.pawar.todo.amt.response.ApiResponse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

/**
 * REST API for the System Logs viewer and runtime log-level management.
 *
 * Endpoints:
 *   GET  /api/system/logs             – recent log entries (JSON)
 *   GET  /api/system/logs/stream      – SSE live stream of new log entries
 *   GET  /api/system/logs/levels      – current effective log levels for key packages
 *   PUT  /api/system/logs/level       – change a package's log level at runtime
 */
@RestController
@RequestMapping("/api/system/logs")
public class SystemLogsController {

    private static final Logger log = LoggerFactory.getLogger(SystemLogsController.class);

    /** Packages exposed for runtime level changes. */
    private static final List<String> MANAGED_LOGGERS = List.of(
            "com.pawar.todo.amt",
            "com.pawar.sop",
            "org.springframework",
            "org.hibernate"
    );

    private final Executor logStreamExecutor;

    public SystemLogsController(Executor logStreamExecutor) {
        this.logStreamExecutor = logStreamExecutor;
    }

    // ── GET /api/system/logs ──────────────────────────────────────────────────

    /**
     * Returns the most recent log entries from the in-memory ring buffer.
     *
     * @param lines    max number of entries to return (default 200, max 2000)
     * @param minLevel minimum level to include: TRACE, DEBUG, INFO, WARN, ERROR
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @GetMapping
    public ResponseEntity<ApiResponse<List<LogEntry>>> getLogs(
            @RequestParam(defaultValue = "200") int lines,
            @RequestParam(defaultValue = "INFO") String minLevel) {

        int safeLines = Math.min(Math.max(lines, 1), 2000);
        List<LogEntry> entries = SystemLogAppender.getRecent(safeLines, minLevel);
        log.debug("Serving {} log entries (minLevel={})", entries.size(), minLevel);
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", entries));
    }

    // ── GET /api/system/logs/stream ───────────────────────────────────────────

    /**
     * SSE live stream of new log entries.
     *
     * Uses {@link SystemLogAppender#totalAppended()} — a monotonically
     * increasing counter — to detect new entries.  This works correctly even
     * after the ring buffer wraps around (i.e. when bufferSize() stays
     * constant at CAPACITY and a pure size-comparison would never fire).
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(0L); // no server-side timeout
        AtomicBoolean stopped = new AtomicBoolean(false);

        // Record the total number of entries at stream-open time.
        // New entries are defined as those appended after this moment.
        AtomicLong lastSeq = new AtomicLong(SystemLogAppender.totalAppended());

        emitter.onCompletion(() -> stopped.set(true));
        emitter.onTimeout(() -> stopped.set(true));
        emitter.onError(e -> stopped.set(true));

        try {
            logStreamExecutor.execute(() -> {
                try {
                    while (!stopped.get()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            return;
                        }

                        long currentSeq = SystemLogAppender.totalAppended();
                        long delta = currentSeq - lastSeq.get();

                        if (delta > 0) {
                            // Fetch only the newly-arrived entries (up to delta, capped at 200)
                            int count = (int) Math.min(delta, 200);
                            List<LogEntry> newEntries = SystemLogAppender.getLast(count);
                            lastSeq.set(currentSeq);

                            for (LogEntry entry : newEntries) {
                                if (stopped.get()) return;
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("log")
                                            .data(entry));
                                } catch (IOException ioEx) {
                                    stopped.set(true);
                                    return; // client disconnected
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    if (!stopped.get()) {
                        emitter.completeWithError(ex);
                    }
                }
            });
        } catch (RejectedExecutionException ex) {
            emitter.completeWithError(
                    new IllegalStateException("Too many active log streams", ex));
        }

        return emitter;
    }

    // ── GET /api/system/logs/levels ───────────────────────────────────────────

    /**
     * Returns the current effective log level for each managed package.
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @GetMapping("/levels")
    public ResponseEntity<ApiResponse<Map<String, String>>> getLevels() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Map<String, String> levels = new LinkedHashMap<>();
        for (String name : MANAGED_LOGGERS) {
            ch.qos.logback.classic.Logger logger = context.getLogger(name);
            Level effective = logger.getEffectiveLevel();
            levels.put(name, effective != null ? effective.toString() : "INFO");
        }
        return ResponseEntity.ok(new ApiResponse<>(true, "OK", levels));
    }

    // ── PUT /api/system/logs/level ────────────────────────────────────────────

    /**
     * Changes the log level of a specific package at runtime.
     *
     * Request body: { "logger": "com.pawar.todo.amt", "level": "DEBUG" }
     *
     * Using {@code Map<String, String>} for the request body avoids any
     * Jackson record-deserialization edge-cases with older Jackson versions.
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @PutMapping("/level")
    public ResponseEntity<ApiResponse<String>> setLevel(
            @RequestBody Map<String, String> body) {

        String loggerName = body == null ? null : body.get("logger");
        String levelStr   = body == null ? null : body.get("level");

        if (loggerName == null || loggerName.isBlank() || levelStr == null || levelStr.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Both 'logger' and 'level' fields are required", null));
        }

        loggerName = loggerName.trim();
        levelStr   = levelStr.trim().toUpperCase();

        // Safety: only allow changes to known managed packages
        final String finalLoggerName = loggerName;
        boolean managed = MANAGED_LOGGERS.stream().anyMatch(finalLoggerName::startsWith);
        if (!managed) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false,
                            "Logger '" + loggerName + "' is not in the managed list", null));
        }

        Level newLevel = Level.toLevel(levelStr, null);
        if (newLevel == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Invalid level: " + levelStr
                            + ". Valid values: TRACE, DEBUG, INFO, WARN, ERROR, OFF", null));
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger(loggerName);
        logger.setLevel(newLevel);

        String message = "Log level for '" + loggerName + "' set to " + newLevel;
        log.info(message);
        return ResponseEntity.ok(new ApiResponse<>(true, message, null));
    }
}
