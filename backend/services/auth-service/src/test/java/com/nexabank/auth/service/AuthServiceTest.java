package com.banking.auth.service;

import com.banking.auth.dto.LoginRequest;
import com.banking.auth.dto.RegisterRequest;
import com.banking.auth.entity.RefreshToken;
import com.banking.auth.entity.Role;
import com.banking.auth.entity.User;
import com.banking.auth.exception.AuthException;
import com.banking.auth.repository.RefreshTokenRepository;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 900_000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 86_400_000L);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("user@test.com");
        testUser.setPassword("hashed_password");
        testUser.setRole(Role.CUSTOMER);
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setFailedLoginAttempts(0);
    }

    @Test
    void register_NewUser_ReturnsAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@test.com");
        request.setPassword("Password@1");

        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@1")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(jwtService.generateAccessToken(any(), any(Map.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

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
        when(authenticationManager.authenticate(any())).thenReturn(
            new UsernamePasswordAuthenticationToken("user@test.com", "correct_password")
        );
        when(jwtService.generateAccessToken(any(), any(Map.class))).thenReturn("access_token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh_token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.login(request);

        assertThat(response.getAccessToken()).isNotNull();
    }

    @Test
    void login_WrongPassword_IncrementsFailedAttempts() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("wrong_password");

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

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
            .isInstanceOf(LockedException.class)
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
