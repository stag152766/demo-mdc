package com.demo.mdc.controller;

import com.demo.mdc.filter.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ── /api/hello ────────────────────────────────────────────────────────────

    @Test
    void hello_returnsOkWithMessage() throws Exception {
        mockMvc.perform(get("/api/hello"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Hello from MDC Demo!"))
            .andExpect(jsonPath("$.rquid").isNotEmpty());
    }

    @Test
    void hello_responseHeaderContainsGeneratedRquid() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/hello"))
            .andExpect(status().isOk())
            .andReturn();

        String headerRquid = result.getResponse().getHeader(RequestIdFilter.RQUID_HEADER);
        assertThat(headerRquid).isNotBlank();

        // rquid in JSON body must match the response header
        assertThat(result.getResponse().getContentAsString()).contains(headerRquid);
    }

    @Test
    void hello_usesClientProvidedRquid() throws Exception {
        String clientRquid = "client-rquid-12345";

        MvcResult result = mockMvc.perform(get("/api/hello")
                .header(RequestIdFilter.RQUID_HEADER, clientRquid))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(result.getResponse().getHeader(RequestIdFilter.RQUID_HEADER))
            .isEqualTo(clientRquid);
        assertThat(result.getResponse().getContentAsString()).contains(clientRquid);
    }

    // ── /api/echo ─────────────────────────────────────────────────────────────

    @Test
    void echo_returnsProcessedMessage() throws Exception {
        String rquid = "echo-rquid-abc";

        MvcResult result = mockMvc.perform(get("/api/echo")
                .param("message", "hello world")
                .header(RequestIdFilter.RQUID_HEADER, rquid))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.echo").value("Processed: hello world"))
            .andReturn();

        assertThat(result.getResponse().getHeader(RequestIdFilter.RQUID_HEADER))
            .isEqualTo(rquid);
    }

    @Test
    void echo_rquidConsistentInHeaderAndBody() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/echo").param("message", "test"))
            .andExpect(status().isOk())
            .andReturn();

        String headerRquid = result.getResponse().getHeader(RequestIdFilter.RQUID_HEADER);
        assertThat(result.getResponse().getContentAsString()).contains(headerRquid);
    }

    // ── /api/process ──────────────────────────────────────────────────────────

    @Test
    void process_returnsProcessedPayload() throws Exception {
        String rquid = "process-rquid-xyz";

        MvcResult result = mockMvc.perform(post("/api/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"value\"}")
                .header(RequestIdFilter.RQUID_HEADER, rquid))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.processed").value(true))
            .andReturn();

        assertThat(result.getResponse().getHeader(RequestIdFilter.RQUID_HEADER))
            .isEqualTo(rquid);
    }

    // ── /api/async ────────────────────────────────────────────────────────────

    @Test
    void async_rquidPropagatedToWorkerThread() throws Exception {
        String rquid = "async-rquid-test-001";

        // Step 1: trigger the async handler
        MvcResult asyncResult = mockMvc.perform(get("/api/async")
                .param("message", "hello")
                .header(RequestIdFilter.RQUID_HEADER, rquid))
            .andExpect(request().asyncStarted())
            .andReturn();

        // Step 2: dispatch the async result and assert
        MvcResult result = mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.asyncRquid").value(rquid))
            .andReturn();

        assertThat(result.getResponse().getHeader(RequestIdFilter.RQUID_HEADER))
            .isEqualTo(rquid);
    }

    @Test
    void async_generatedRquidPropagatedWhenNoHeaderProvided() throws Exception {
        // Step 1
        MvcResult asyncResult = mockMvc.perform(get("/api/async").param("message", "test"))
            .andExpect(request().asyncStarted())
            .andReturn();

        String headerRquid = asyncResult.getResponse().getHeader(RequestIdFilter.RQUID_HEADER);
        assertThat(headerRquid).isNotBlank();

        // Step 2
        MvcResult result = mockMvc.perform(asyncDispatch(asyncResult))
            .andExpect(status().isOk())
            .andReturn();

        // asyncRquid seen on the worker thread must equal the header value
        assertThat(result.getResponse().getContentAsString()).contains(headerRquid);
    }

    @Test
    void process_rquidConsistentInHeaderAndBody() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"data\":\"demo\"}"))
            .andExpect(status().isOk())
            .andReturn();

        String headerRquid = result.getResponse().getHeader(RequestIdFilter.RQUID_HEADER);
        assertThat(result.getResponse().getContentAsString()).contains(headerRquid);
    }
}
