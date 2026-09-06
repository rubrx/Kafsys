package com.kafsys.common.tracing;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdServletFilterTest {

    private final CorrelationIdServletFilter filter = new CorrelationIdServletFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void honorsIncomingHeader_andPropagatesToMdcAndResponse() throws Exception {
        String incoming = "trace-42";
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
        req.addHeader(CorrelationId.HTTP_HEADER, incoming);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        AtomicReference<String> mdcDuringDispatch = new AtomicReference<>();
        FilterChain chain = (r, s) -> mdcDuringDispatch.set(MDC.get(CorrelationId.MDC_KEY));

        filter.doFilter(req, resp, chain);

        assertThat(mdcDuringDispatch.get()).isEqualTo(incoming);
        assertThat(resp.getHeader(CorrelationId.HTTP_HEADER)).isEqualTo(incoming);
        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }

    @Test
    void generatesFreshUuid_whenHeaderAbsent() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, resp, chain);

        String responseHeader = resp.getHeader(CorrelationId.HTTP_HEADER);
        assertThat(responseHeader).isNotBlank();
        // Confirm UUID shape.
        assertThat(UUID.fromString(responseHeader)).isNotNull();
    }

    @Test
    void mdcIsRestored_evenWhenChainThrows() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/");
        req.addHeader(CorrelationId.HTTP_HEADER, "boom-id");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        FilterChain chain = (r, s) -> { throw new RuntimeException("fail"); };

        try {
            filter.doFilter(req, resp, chain);
        } catch (Exception ignored) { }

        assertThat(MDC.get(CorrelationId.MDC_KEY)).isNull();
    }
}
