package com.kafsys.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    // Rate-limit key: per IP address (falls back to "anonymous" if header absent)
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (ip == null) {
                ip = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "anonymous";
            }
            return Mono.just(ip);
        };
    }
}
