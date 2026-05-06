package com.kafsys.identity.controller;

import com.kafsys.common.dto.ApiResponse;
import com.kafsys.identity.dto.LoginRequest;
import com.kafsys.identity.dto.LoginResponse;
import com.kafsys.identity.dto.RegisterRequest;
import com.kafsys.identity.dto.TokenRefreshRequest;
import com.kafsys.identity.dto.TokenRefreshResponse;
import com.kafsys.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "JWT authentication and user registration endpoints")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Returns JWT access token and refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Authentication successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Creates a new customer account")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(null, "User registered successfully"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Issues a new access token using a valid refresh token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("X-Auth-UserId") String userId) {
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }
}
