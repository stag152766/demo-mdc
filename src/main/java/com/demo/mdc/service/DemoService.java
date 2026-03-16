package com.demo.mdc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class DemoService {

    private static final Logger log = LoggerFactory.getLogger(DemoService.class);

    public String processMessage(String message) {
        log.info("DemoService processing message: '{}'", message);
        return "Processed: " + message;
    }

    public String getCurrentRquid() {
        String rquid = MDC.get("rquid");
        log.info("DemoService getCurrentRquid -> {}", rquid);
        return rquid;
    }

    /**
     * Runs on the async thread pool. MDC is propagated by MdcTaskDecorator,
     * so the rquid logged here must match the one from the HTTP request.
     */
    @Async
    public CompletableFuture<String> processAsync(String message) {
        String rquid = MDC.get("rquid");
        log.info("DemoService.processAsync on thread '{}', rquid={}", Thread.currentThread().getName(), rquid);
        return CompletableFuture.completedFuture(rquid);
    }
}
