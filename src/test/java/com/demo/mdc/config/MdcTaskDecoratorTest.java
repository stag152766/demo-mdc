package com.demo.mdc.config;

import com.demo.mdc.filter.RequestIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    void propagatesMdcToWorkerThread() throws InterruptedException {
        MDC.put(RequestIdFilter.RQUID_MDC_KEY, "propagate-me");

        AtomicReference<String> capturedOnWorker = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Runnable decorated = decorator.decorate(() -> {
            capturedOnWorker.set(MDC.get(RequestIdFilter.RQUID_MDC_KEY));
            latch.countDown();
        });

        Thread worker = new Thread(decorated);
        worker.start();
        latch.await();

        assertThat(capturedOnWorker.get()).isEqualTo("propagate-me");
    }

    @Test
    void clearsMdcOnWorkerThreadAfterTaskCompletes() throws InterruptedException {
        MDC.put(RequestIdFilter.RQUID_MDC_KEY, "must-be-cleared");

        AtomicReference<String> afterTask = new AtomicReference<>("sentinel");
        CountDownLatch latch = new CountDownLatch(1);

        Runnable decorated = decorator.decorate(() -> {
            // task body – MDC is populated here
        });

        Thread worker = new Thread(() -> {
            decorated.run();
            // after run() returns – MDC must be cleared
            afterTask.set(MDC.get(RequestIdFilter.RQUID_MDC_KEY));
            latch.countDown();
        });
        worker.start();
        latch.await();

        assertThat(afterTask.get()).isNull();
    }

    @Test
    void workerHasEmptyMdcWhenCallerHadNone() throws InterruptedException {
        MDC.clear(); // caller has no MDC

        AtomicReference<String> capturedOnWorker = new AtomicReference<>("sentinel");
        CountDownLatch latch = new CountDownLatch(1);

        Runnable decorated = decorator.decorate(() -> {
            capturedOnWorker.set(MDC.get(RequestIdFilter.RQUID_MDC_KEY));
            latch.countDown();
        });

        new Thread(decorated).start();
        latch.await();

        assertThat(capturedOnWorker.get()).isNull();
    }
}
