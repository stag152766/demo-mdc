package com.demo.mdc.aspect;

import com.demo.mdc.filter.RequestIdFilter;
import com.demo.mdc.service.DemoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for LoggerMdcAspect – verified through DemoService which is in a
 * matched package and will be proxied by the aspect.
 */
@SpringBootTest
class LoggerMdcAspectTest {

    @Autowired
    private DemoService demoService;

    @AfterEach
    void cleanup() {
        MDC.remove(RequestIdFilter.RQUID_MDC_KEY);
    }

    @Test
    void aspectGeneratesRquidWhenMdcIsEmpty() {
        MDC.remove(RequestIdFilter.RQUID_MDC_KEY);

        // DemoService.getCurrentRquid reads MDC inside the method.
        // The aspect fires before the method, so MDC must be populated.
        String captured = demoService.getCurrentRquid();

        assertThat(captured).isNotBlank();
    }

    @Test
    void aspectPreservesExistingRquidSetByFilter() {
        String existing = "filter-set-rquid-abc";
        MDC.put(RequestIdFilter.RQUID_MDC_KEY, existing);

        String captured = demoService.getCurrentRquid();

        assertThat(captured).isEqualTo(existing);
        // Aspect must NOT remove a rquid it didn't create
        assertThat(MDC.get(RequestIdFilter.RQUID_MDC_KEY)).isEqualTo(existing);
    }

    @Test
    void aspectRemovesGeneratedRquidAfterMethod() {
        MDC.remove(RequestIdFilter.RQUID_MDC_KEY);

        demoService.getCurrentRquid();

        // Aspect owns the context → must clean up after itself
        assertThat(MDC.get(RequestIdFilter.RQUID_MDC_KEY)).isNull();
    }

    @Test
    void aspectDoesNotRemoveExternalRquidAfterMethod() {
        String existing = "should-survive-abc";
        MDC.put(RequestIdFilter.RQUID_MDC_KEY, existing);

        demoService.processMessage("hello");

        assertThat(MDC.get(RequestIdFilter.RQUID_MDC_KEY)).isEqualTo(existing);
    }
}
