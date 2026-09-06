package com.kafsys.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String INSECURE_DEV_SENTINEL =
            "INSECURE-DEV-SECRET-DO-NOT-USE-IN-PROD-8f2c1a4b6e9d3c7a5f1b8e2c4d6a9f3b";
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${kafsys.jwt.secret}")
    private String jwtSecret;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @PostConstruct
    void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("kafsys.jwt.secret / JWT_SECRET must be set (>= 32 chars)");
        }
        if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "kafsys.jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
        if (INSECURE_DEV_SENTINEL.equals(jwtSecret)) {
            log.warn("=====================================================================");
            log.warn("SECURITY WARNING: JWT_SECRET is using the INSECURE dev-only default.");
            log.warn("Export JWT_SECRET before running outside local development.");
            log.warn("=====================================================================");
        }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return unauthorized(exchange);
            }

            String token = authHeader.substring(BEARER_PREFIX.length());
            try {
                Claims claims = parseToken(token);
                String subject = claims.getSubject();
                String roles = claims.get("roles", String.class);

                var mutatedRequest = exchange.getRequest().mutate()
                        .header("X-Auth-UserId", subject)
                        .header("X-Auth-Roles", roles != null ? roles : "")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (JwtException e) {
                log.warn("Invalid JWT token: {}", e.getMessage());
                return unauthorized(exchange);
            }
        };
    }

    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> unauthorized(org.springframework.web.server.ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var body = exchange.getResponse().bufferFactory()
                .wrap("{\"success\":false,\"error\":\"Unauthorized — valid JWT required\"}".getBytes());
        return exchange.getResponse().writeWith(Mono.just(body));
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of();
    }

    public static class Config {
    }
}
