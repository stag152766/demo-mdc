package com.demo.mdc.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * Copies the MDC context map from the submitting thread to the worker thread
 * so that rquid (and any other MDC values) survive the thread-pool handoff.
 *
 * Without this decorator every @Async method would start with an empty MDC,
 * making log correlation across sync/async boundaries impossible.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Capture the MDC snapshot on the CALLING thread (request thread)
        Map<String, String> mdcSnapshot = MDC.getCopyOfContextMap();

        return () -> {
            // Restore it on the WORKER thread before the task runs
            if (mdcSnapshot != null) {
                MDC.setContextMap(mdcSnapshot);
            }
            try {
                runnable.run();
            } finally {
                // Always clear to avoid leaking MDC state into the next task
                // that reuses this worker thread
                MDC.clear();
            }
        };
    }
}
