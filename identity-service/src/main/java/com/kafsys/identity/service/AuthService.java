package com.kafsys.identity.service;

import com.kafsys.common.exception.ResourceNotFoundException;
import com.kafsys.identity.dto.LoginRequest;
import com.kafsys.identity.dto.LoginResponse;
import com.kafsys.identity.dto.RegisterRequest;
import com.kafsys.identity.dto.TokenRefreshRequest;
import com.kafsys.identity.dto.TokenRefreshResponse;
import com.kafsys.identity.entity.Role;
import com.kafsys.identity.entity.User;
import com.kafsys.identity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        log.info("User authenticated: username={}, role={}", user.getUsername(), user.getRole());

        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtService.getRefreshTokenExpiryMs() / 1000,
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken: " + request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }

        User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.ROLE_CUSTOMER
        );
        userRepository.save(user);
        log.info("New user registered: username={}", request.username());
    }

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        User user = userRepository.findByRefreshToken(request.refreshToken())
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token", "provided token"));

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken();
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshToken(null);
            userRepository.save(user);
        });
        log.info("User logged out: userId={}", userId);
    }
}
