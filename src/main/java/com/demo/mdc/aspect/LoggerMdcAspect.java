package com.demo.mdc.aspect;

import com.demo.mdc.filter.RequestIdFilter;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ensures rquid is present in MDC for every public method in the application.
 *
 * In a normal web request the filter already populates MDC before this aspect
 * fires, so @Before is a no-op for those methods. For code paths that run
 * outside a web context (scheduled tasks, async threads, plain unit tests) the
 * aspect generates a fresh rquid.
 *
 * A ThreadLocal bridges the @Before / @After boundary so that only the advice
 * that set the value is the one that removes it.
 */
@Aspect
@Component
public class LoggerMdcAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggerMdcAspect.class);

    /**
     * Tracks whether THIS advice invocation put the rquid into MDC.
     * Must be cleaned up in @After to avoid ThreadLocal leaks.
     */
    private static final ThreadLocal<Boolean> ownsContext =
            ThreadLocal.withInitial(() -> false);

    /**
     * All public methods in the application packages, excluding the aspect
     * itself and the filter (which owns the MDC lifecycle for web requests).
     */
    @Pointcut("execution(public * com.demo.mdc..*(..)) " +
              "&& !within(com.demo.mdc.aspect..*) " +
              "&& !within(com.demo.mdc.filter..*)")
    public void applicationPublicMethods() {}

    @Before("applicationPublicMethods()")
    public void setMdcRquid(JoinPoint jp) {
        if (MDC.get(RequestIdFilter.RQUID_MDC_KEY) == null) {
            String generated = UUID.randomUUID().toString();
            MDC.put(RequestIdFilter.RQUID_MDC_KEY, generated);
            ownsContext.set(true);
            log.debug("Aspect set rquid={} for {}", generated, jp.getSignature().toShortString());
        }
    }

    @After("applicationPublicMethods()")
    public void clearMdcRquid(JoinPoint jp) {
        if (ownsContext.get()) {
            MDC.remove(RequestIdFilter.RQUID_MDC_KEY);
            ownsContext.remove();   // prevent ThreadLocal leak in thread-pool threads
        }
    }
}
