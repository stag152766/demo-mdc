package com.demo.mdc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

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
}
