package com.kafsys.identity.service;

import com.kafsys.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String INSECURE_DEV_SENTINEL =
            "INSECURE-DEV-SECRET-DO-NOT-USE-IN-PROD-8f2c1a4b6e9d3c7a5f1b8e2c4d6a9f3b";
    private static final int MIN_SECRET_BYTES = 32;

    @Value("${kafsys.jwt.secret}")
    private String jwtSecret;

    @Value("${kafsys.jwt.access-token-expiry-ms:900000}")
    private long accessTokenExpiryMs;

    @Value("${kafsys.jwt.refresh-token-expiry-ms:604800000}")
    private long refreshTokenExpiryMs;

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

    public String generateAccessToken(User user) {
        SecretKey key = signingKey();
        return Jwts.builder()
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("roles", user.getRole().name())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(accessTokenExpiryMs)))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString().replace("-", "") +
               UUID.randomUUID().toString().replace("-", "");
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseAccessToken(token);
            return !claims.getExpiration().before(Date.from(Instant.now()));
        } catch (Exception e) {
            return false;
        }
    }

    public long getRefreshTokenExpiryMs() {
        return refreshTokenExpiryMs;
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
