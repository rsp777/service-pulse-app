package com.pawar.todo.amt.controller;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

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
     * SSE stream: polls the in-memory buffer every 1 second and pushes new
     * entries since the last poll.
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        AtomicInteger lastSize = new AtomicInteger(SystemLogAppender.bufferSize());

        try {
            logStreamExecutor.execute(() -> {
                try {
                    while (true) {
                        try { Thread.sleep(1000); } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        int currentSize = SystemLogAppender.bufferSize();
                        if (currentSize != lastSize.get()) {
                            List<LogEntry> newEntries = SystemLogAppender.getSince(lastSize.get());
                            lastSize.set(currentSize);
                            for (LogEntry entry : newEntries) {
                                try {
                                    emitter.send(SseEmitter.event()
                                            .name("log")
                                            .data(entry));
                                } catch (IOException ioEx) {
                                    return; // client disconnected
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            });
        } catch (RejectedExecutionException ex) {
            emitter.completeWithError(new IllegalStateException("Too many active log streams", ex));
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
     */
    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @PutMapping("/level")
    public ResponseEntity<ApiResponse<Map<String, String>>> setLevel(
            @RequestBody LogLevelRequest request) {

        if (request == null || request.logger() == null || request.level() == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "logger and level are required", null));
        }

        // Only allow changes to managed loggers for safety
        String loggerName = request.logger().trim();
        boolean managed = MANAGED_LOGGERS.stream().anyMatch(loggerName::startsWith);
        if (!managed) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false,
                            "Logger '" + loggerName + "' is not in the managed list", null));
        }

        Level newLevel = Level.toLevel(request.level().trim().toUpperCase(), null);
        if (newLevel == null) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "Invalid level: " + request.level(), null));
        }

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger logger = context.getLogger(loggerName);
        logger.setLevel(newLevel);

        log.info("Log level changed: logger='{}' level='{}'", loggerName, newLevel);

        return ResponseEntity.ok(new ApiResponse<>(true,
                "Log level for '" + loggerName + "' set to " + newLevel, null));
    }

    // ── Inner record for request body ─────────────────────────────────────────

    public record LogLevelRequest(String logger, String level) {}
}
