package com.kafsys.common.tracing;

/**
 * Shared constants and helpers for propagating a request-scoped correlation ID
 * across HTTP boundaries, log lines (via SLF4J MDC), and Kafka message headers.
 *
 * <p>The header name mirrors the informal cross-language convention
 * ("X-Correlation-Id"); the MDC key is unadorned ("correlationId") so log
 * pattern layouts stay short.
 */
public final class CorrelationId {

    public static final String HTTP_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";
    public static final String KAFKA_HEADER = "X-Correlation-Id";

    private CorrelationId() {}
}
