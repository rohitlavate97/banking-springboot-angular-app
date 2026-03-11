package com.nexabank.auth.service;

import com.nexabank.auth.dto.LoginRequest;
import com.nexabank.auth.dto.RegisterRequest;
import com.nexabank.auth.entity.Role;
import com.nexabank.auth.entity.User;
import com.nexabank.auth.exception.AuthException;
import com.nexabank.auth.repository.RefreshTokenRepository;
import com.nexabank.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@test.com");
        testUser.setPassword("hashed_password");
        testUser.setRole(Role.USER);
        testUser.setEnabled(true);
        testUser.setFailedLoginAttempts(0);
    }

    @Test
    void register_NewUser_ReturnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@test.com");
        request.setPassword("Password@1");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(jwtService.generateToken(any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");

        var response = authService.register(request);

        assertThat(response.getAccessToken()).isEqualTo("access_token");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_DuplicateEmail_ThrowsAuthException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@test.com");

        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(AuthException.class)
            .hasMessageContaining("already");
    }

    @Test
    void login_ValidCredentials_ReturnsTokens() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("correct_password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correct_password", "hashed_password")).thenReturn(true);
        when(jwtService.generateToken(any())).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");

        var response = authService.login(request);

        assertThat(response.getAccessToken()).isNotNull();
        verify(userRepository).save(testUser); // reset failed attempts
    }

    @Test
    void login_WrongPassword_IncrementsFailedAttempts() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("wrong_password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthException.class);

        assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
        verify(userRepository).save(testUser);
    }

    @Test
    void login_AccountLockedAfterFiveFailures_ThrowsLockedException() {
        testUser.setFailedLoginAttempts(5);
        testUser.setAccountNonLocked(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("any");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthException.class)
            .hasMessageContaining("locked");
    }

    @Test
    void login_UserNotFound_ThrowsAuthException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("ghost@test.com");
        request.setPassword("password");

        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(AuthException.class);
    }
}
