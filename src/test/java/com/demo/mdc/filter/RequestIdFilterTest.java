package com.demo.mdc.filter;

import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @AfterEach
    void cleanup() {
        MDC.remove(RequestIdFilter.RQUID_MDC_KEY);
    }

    @Test
    void generatesUuidWhenHeaderAbsent() throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain    = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String header = response.getHeader(RequestIdFilter.RQUID_HEADER);
        assertThat(header).isNotBlank();
        assertThat(header).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void reusesRquidFromRequestHeader() throws Exception {
        String providedRquid = "my-custom-rquid-001";
        var request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.RQUID_HEADER, providedRquid);
        var response = new MockHttpServletResponse();
        var chain    = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.RQUID_HEADER)).isEqualTo(providedRquid);
    }

    @Test
    void setsRquidInMdcDuringChain() throws Exception {
        String[] mdcDuringRequest = new String[1];
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        // Use MockFilterChain(Servlet, Filter...) – the capturing Filter fires last
        var chain = new MockFilterChain(new HttpServlet() {},
            (req, resp, ch) -> mdcDuringRequest[0] = MDC.get(RequestIdFilter.RQUID_MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(mdcDuringRequest[0]).isNotBlank();
    }

    @Test
    void responseHeaderMatchesMdcValueDuringChain() throws Exception {
        String[] mdcDuringRequest = new String[1];
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain(new HttpServlet() {},
            (req, resp, ch) -> mdcDuringRequest[0] = MDC.get(RequestIdFilter.RQUID_MDC_KEY));

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(RequestIdFilter.RQUID_HEADER))
            .isEqualTo(mdcDuringRequest[0]);
    }

    @Test
    void clearsMdcAfterRequest() throws Exception {
        var request  = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var chain    = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(MDC.get(RequestIdFilter.RQUID_MDC_KEY)).isNull();
    }
}
