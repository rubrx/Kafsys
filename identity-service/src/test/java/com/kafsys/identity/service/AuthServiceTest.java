package com.kafsys.identity.service;

import com.kafsys.identity.dto.LoginRequest;
import com.kafsys.identity.dto.LoginResponse;
import com.kafsys.identity.dto.RegisterRequest;
import com.kafsys.identity.dto.TokenRefreshRequest;
import com.kafsys.identity.dto.TokenRefreshResponse;
import com.kafsys.identity.entity.Role;
import com.kafsys.identity.entity.User;
import com.kafsys.identity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;

    @InjectMocks AuthService auth;

    private User existing;

    @BeforeEach
    void setUp() {
        existing = new User("alice", "alice@example.com", "hashedPw", Role.ROLE_CUSTOMER);
        ReflectionTestUtils.setField(existing, "id", "user-1");
        existing.setEnabled(true);
    }

    @Test
    void login_success_persistsRefreshToken_andReturnsAccessToken() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(encoder.matches("secret", "hashedPw")).thenReturn(true);
        when(jwt.generateAccessToken(existing)).thenReturn("access.jwt");
        when(jwt.generateRefreshToken()).thenReturn("refresh-abc");
        when(jwt.getRefreshTokenExpiryMs()).thenReturn(60_000L);

        LoginResponse response = auth.login(new LoginRequest("alice", "secret"));

        assertThat(response.accessToken()).isEqualTo("access.jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh-abc");
        assertThat(response.expiresIn()).isEqualTo(60L);
        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.role()).isEqualTo("ROLE_CUSTOMER");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getRefreshToken()).isEqualTo("refresh-abc");
    }

    @Test
    void login_wrongPassword_throws() {
        when(users.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(encoder.matches("nope", "hashedPw")).thenReturn(false);

        assertThatThrownBy(() -> auth.login(new LoginRequest("alice", "nope")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid");
        verify(users, never()).save(any());
    }

    @Test
    void login_unknownUser_throws() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.login(new LoginRequest("ghost", "pw")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_disabledUser_throws() {
        existing.setEnabled(false);
        when(users.findByUsername("alice")).thenReturn(Optional.of(existing));
        when(encoder.matches("secret", "hashedPw")).thenReturn(true);

        assertThatThrownBy(() -> auth.login(new LoginRequest("alice", "secret")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void register_duplicateUsername_throws() {
        when(users.existsByUsername("alice")).thenReturn(true);
        assertThatThrownBy(() ->
                auth.register(new RegisterRequest("alice", "a@a.io", "password1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username already taken");
        verify(users, never()).save(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        when(users.existsByUsername("bob")).thenReturn(false);
        when(users.existsByEmail("dup@a.io")).thenReturn(true);
        assertThatThrownBy(() ->
                auth.register(new RegisterRequest("bob", "dup@a.io", "password1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void register_success_encodesPasswordAndSaves() {
        when(users.existsByUsername("bob")).thenReturn(false);
        when(users.existsByEmail("bob@a.io")).thenReturn(false);
        when(encoder.encode("password1")).thenReturn("encoded");

        auth.register(new RegisterRequest("bob", "bob@a.io", "password1"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getUsername()).isEqualTo("bob");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ROLE_CUSTOMER);
    }

    @Test
    void refreshToken_rotatesBothTokens() {
        when(users.findByRefreshToken("old-refresh")).thenReturn(Optional.of(existing));
        when(jwt.generateAccessToken(existing)).thenReturn("new-access");
        when(jwt.generateRefreshToken()).thenReturn("new-refresh");

        TokenRefreshResponse response = auth.refreshToken(new TokenRefreshRequest("old-refresh"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
        assertThat(existing.getRefreshToken()).isEqualTo("new-refresh");
        verify(users).save(existing);
    }

    @Test
    void logout_clearsRefreshToken_whenUserExists() {
        existing.setRefreshToken("active");
        when(users.findById("user-1")).thenReturn(Optional.of(existing));

        auth.logout("user-1");

        assertThat(existing.getRefreshToken()).isNull();
        verify(users).save(existing);
    }

    @Test
    void logout_isNoOp_whenUserMissing() {
        when(users.findById("ghost")).thenReturn(Optional.empty());
        auth.logout("ghost");
        verify(users, never()).save(any());
    }
}
