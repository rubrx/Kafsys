package com.kafsys.gateway.filter;

import com.kafsys.common.tracing.CorrelationId;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Ingress correlation ID for the Kafsys platform.
 *
 * <p>For every request that hits the gateway, either honors an inbound
 * {@link CorrelationId#HTTP_HEADER} (client-supplied trace ID) or generates
 * a new UUID. The ID is attached to the mutated downstream request and to
 * the response, so both callers and internal services observe the same
 * correlation ID for a given request.
 */
@Component
public class CorrelationIdGlobalFilter implements GlobalFilter, Ordered {

    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 5;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(CorrelationId.HTTP_HEADER);
        String id = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;

        var mutatedRequest = exchange.getRequest().mutate()
                .header(CorrelationId.HTTP_HEADER, id)
                .build();
        exchange.getResponse().getHeaders().set(CorrelationId.HTTP_HEADER, id);

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
