package com.kafsys.common.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads an inbound {@link CorrelationId#HTTP_HEADER}, generates one if absent,
 * publishes it to SLF4J MDC for the duration of the request, and echoes it on
 * the response so callers can stitch client-side logs to server-side traces.
 *
 * <p>Runs before any Spring MVC controller / interceptor. Downstream services
 * receive the ID via the Feign/RestClient outbound header propagation (added
 * where those clients are wired up), and Kafka producers pull it out of MDC
 * via {@link KafkaCorrelationInterceptors.Producer}.
 */
public class CorrelationIdServletFilter extends OncePerRequestFilter implements Ordered {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String incoming = request.getHeader(CorrelationId.HTTP_HEADER);
        String id = (incoming == null || incoming.isBlank()) ? newId() : incoming;
        MDC.put(CorrelationId.MDC_KEY, id);
        response.setHeader(CorrelationId.HTTP_HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
