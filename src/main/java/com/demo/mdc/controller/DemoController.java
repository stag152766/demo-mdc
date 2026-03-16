package com.demo.mdc.controller;

import com.demo.mdc.service.DemoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {

    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    /** Simple hello – demonstrates rquid flowing through all layers. */
    @GetMapping("/hello")
    public ResponseEntity<Map<String, String>> hello() {
        log.info("GET /api/hello");
        String rquid = MDC.get("rquid");
        return ResponseEntity.ok(Map.of(
            "message", "Hello from MDC Demo!",
            "rquid",   rquid != null ? rquid : "not-set"
        ));
    }

    /** Echo endpoint – passes a message through DemoService to show the aspect in action. */
    @GetMapping("/echo")
    public ResponseEntity<Map<String, String>> echo(@RequestParam String message) {
        log.info("GET /api/echo message='{}'", message);
        String processed = demoService.processMessage(message);
        String rquid = MDC.get("rquid");
        return ResponseEntity.ok(Map.of(
            "echo",  processed,
            "rquid", rquid != null ? rquid : "not-set"
        ));
    }

    /** Process endpoint – accepts a JSON body and demonstrates rquid on POST. */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> process(@RequestBody Map<String, Object> payload) {
        log.info("POST /api/process payload={}", payload);
        String rquid = demoService.getCurrentRquid();
        log.info("Processing complete, rquid={}", rquid);
        return ResponseEntity.ok(Map.of(
            "processed", true,
            "rquid",     rquid != null ? rquid : "not-set",
            "input",     payload
        ));
    }
}
