package io.github.lonevertex.smscguard;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralizes diagnostic gating, redaction, and bounded event throttling.
 * Messages are limited to stable event names and non-sensitive metadata.
 */
final class DiagnosticLogger {
    private static final int MAX_EVENT_KEYS = 64;
    private final String tag;
    private final Map<String, Long> lastLoggedAt = new ConcurrentHashMap<>();
    private volatile boolean diagnosticsEnabled;

    DiagnosticLogger(String tag) {
        this.tag = tag;
    }

    void setDiagnosticsEnabled(boolean enabled) {
        diagnosticsEnabled = enabled;
    }

    void info(String eventName) {
        try {
            Log.i(tag, "event=" + eventName);
        } catch (Throwable ignored) {
            // Fallback for JVM environments without mocked android.util.Log
        }
    }

    void diagnostic(String eventName, String metadata) {
        if (diagnosticsEnabled) {
            try {
                Log.d(tag, "event=" + eventName + " " + metadata);
            } catch (Throwable ignored) {
                // Fallback for JVM environments without mocked android.util.Log
            }
        }
    }

    void throttledDiagnostic(String eventName, String metadata, long throttleMs) {
        if (diagnosticsEnabled && shouldLog(eventName, throttleMs)) {
            diagnostic(eventName, metadata);
        }
    }

    void throttledFailure(String operation, Throwable throwable, long throttleMs) {
        String eventKey = "reflection_failure:" + operation + ":" + throwable.getClass().getName();
        if (shouldLog(eventKey, throttleMs)) {
            diagnostic("reflection_failure", "operation=" + operation);
        }
    }

    private boolean shouldLog(String eventKey, long throttleMs) {
        long now = System.currentTimeMillis();
        synchronized (lastLoggedAt) {
            Long previous = lastLoggedAt.get(eventKey);
            if (previous != null && now - previous < throttleMs) {
                return false;
            }
            if (!lastLoggedAt.containsKey(eventKey) && lastLoggedAt.size() >= MAX_EVENT_KEYS) {
                lastLoggedAt.clear();
            }
            lastLoggedAt.put(eventKey, now);
            return true;
        }
    }
}
