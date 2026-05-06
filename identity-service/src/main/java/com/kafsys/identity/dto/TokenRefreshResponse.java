package com.kafsys.identity.dto;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {
}
