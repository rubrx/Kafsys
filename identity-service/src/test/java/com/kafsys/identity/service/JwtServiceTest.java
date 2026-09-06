package com.kafsys.identity.service;

import com.kafsys.identity.entity.Role;
import com.kafsys.identity.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String VALID_SECRET =
            "test-secret-that-is-at-least-thirty-two-bytes-long-for-hs256-signing";

    private JwtService jwt;

    @BeforeEach
    void setUp() {
        jwt = new JwtService();
        ReflectionTestUtils.setField(jwt, "jwtSecret", VALID_SECRET);
        ReflectionTestUtils.setField(jwt, "accessTokenExpiryMs", 60_000L);
        ReflectionTestUtils.setField(jwt, "refreshTokenExpiryMs", 3_600_000L);
        jwt.validateSecret();
    }

    private User user() {
        User u = new User("alice", "alice@example.com", "hash", Role.ROLE_CUSTOMER);
        ReflectionTestUtils.setField(u, "id", "user-1");
        return u;
    }

    @Test
    void generateAndParse_roundtrip_carriesClaims() {
        String token = jwt.generateAccessToken(user());
        Claims claims = jwt.parseAccessToken(token);

        assertThat(claims.getSubject()).isEqualTo("user-1");
        assertThat(claims.get("username", String.class)).isEqualTo("alice");
        assertThat(claims.get("roles", String.class)).isEqualTo("ROLE_CUSTOMER");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void isTokenValid_returnsFalse_forTamperedToken() {
        String token = jwt.generateAccessToken(user()) + "TAMPERED";
        assertThat(jwt.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_returnsTrue_forFreshToken() {
        String token = jwt.generateAccessToken(user());
        assertThat(jwt.isTokenValid(token)).isTrue();
    }

    @Test
    void generateRefreshToken_producesLongOpaqueString() {
        String refresh = jwt.generateRefreshToken();
        assertThat(refresh).hasSize(64).doesNotContain("-");
    }

    @Test
    void validateSecret_rejectsBlankSecret() {
        JwtService bad = new JwtService();
        ReflectionTestUtils.setField(bad, "jwtSecret", "  ");
        assertThatThrownBy(bad::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be set");
    }

    @Test
    void validateSecret_rejectsShortSecret() {
        JwtService bad = new JwtService();
        ReflectionTestUtils.setField(bad, "jwtSecret", "too-short");
        assertThatThrownBy(bad::validateSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least");
    }
}
