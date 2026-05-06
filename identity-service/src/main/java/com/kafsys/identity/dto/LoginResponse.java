package com.kafsys.identity.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String userId,
        String username,
        String role
) {
}
