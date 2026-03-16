package com.demo.mdc.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(1)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    public static final String RQUID_HEADER  = "X-Request-ID";
    public static final String RQUID_MDC_KEY = "rquid";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String rquid = request.getHeader(RQUID_HEADER);

        if (!StringUtils.hasText(rquid)) {
            rquid = UUID.randomUUID().toString();
            log.debug("Generated new rquid: {}", rquid);
        } else {
            log.debug("Reusing rquid from request header: {}", rquid);
        }

        MDC.put(RQUID_MDC_KEY, rquid);
        response.setHeader(RQUID_HEADER, rquid);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RQUID_MDC_KEY);
        }
    }
}
