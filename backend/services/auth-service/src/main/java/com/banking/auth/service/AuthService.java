package com.banking.auth.service;

import com.banking.auth.dto.*;
import com.banking.auth.entity.RefreshToken;
import com.banking.auth.entity.Role;
import com.banking.auth.entity.User;
import com.banking.auth.exception.AuthException;
import com.banking.auth.repository.RefreshTokenRepository;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.CUSTOMER)
                .enabled(true)
                .accountNonLocked(true)
                .failedLoginAttempts(0)
                .build();

        userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return generateTokenPair(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!user.isAccountNonLocked()) {
            log.warn("Login attempt for locked account: {}", request.getEmail());
            throw new LockedException("Account is locked. Try again later.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            handleFailedLogin(user);
            throw new AuthException("Invalid credentials");
        }

        resetFailedAttempts(user);
        refreshTokenRepository.deleteExpiredAndRevokedByUser(user);

        log.info("User logged in: {}", user.getEmail());
        return generateTokenPair(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (storedToken.isRevoked() || storedToken.isExpired()) {
            log.warn("Attempted use of revoked or expired refresh token for user: {}", storedToken.getUser().getEmail());
            refreshTokenRepository.revokeAllByUser(storedToken.getUser());
            throw new AuthException("Refresh token is invalid or expired");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        log.info("Token refreshed for user: {}", user.getEmail());
        return generateTokenPair(user);
    }

    @Transactional
    public void logout(String accessToken) {
        String email = jwtService.extractUsername(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthException("User not found"));
        refreshTokenRepository.revokeAllByUser(user);
        log.info("User logged out: {}", email);
    }

    private AuthResponse generateTokenPair(User user) {
        Map<String, Object> claims = Map.of("role", user.getRole().name());
        String accessToken = jwtService.generateAccessToken(user, claims);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000)
                .role(user.getRole().name())
                .email(user.getEmail())
                .build();
    }

    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountNonLocked(false);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked after {} failed attempts: {}", attempts, user.getEmail());
        }
        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setAccountNonLocked(true);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }
}
