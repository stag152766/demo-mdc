package com.demo.mdc.aspect;

import com.demo.mdc.filter.RequestIdFilter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
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
 * fires, so the aspect is a no-op for those methods. For code paths that run
 * outside a web context (scheduled tasks, async threads, plain unit tests) the
 * aspect generates a fresh rquid and removes it when the method returns.
 */
@Aspect
@Component
public class LoggerMdcAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggerMdcAspect.class);

    /**
     * All public methods in the application packages, excluding the aspect
     * itself and the filter (which owns the MDC lifecycle for web requests).
     */
    @Pointcut("execution(public * com.demo.mdc..*(..)) " +
              "&& !within(com.demo.mdc.aspect..*) " +
              "&& !within(com.demo.mdc.filter..*)")
    public void applicationPublicMethods() {}

    @Around("applicationPublicMethods()")
    public Object ensureMdcRquid(ProceedingJoinPoint pjp) throws Throwable {
        boolean ownsContext = false;

        if (MDC.get(RequestIdFilter.RQUID_MDC_KEY) == null) {
            String generated = UUID.randomUUID().toString();
            MDC.put(RequestIdFilter.RQUID_MDC_KEY, generated);
            ownsContext = true;
            log.debug("Aspect set rquid={} for {}", generated, pjp.getSignature().toShortString());
        }

        try {
            return pjp.proceed();
        } finally {
            if (ownsContext) {
                MDC.remove(RequestIdFilter.RQUID_MDC_KEY);
            }
        }
    }
}
