package com.banking.auth.service;

import com.banking.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    // 64-byte hex-encoded test secret (256-bit key encoded as hex = 64 hex chars)
    private static final String TEST_SECRET =
        "7465737453656372657400000000000000000000000000000000000000000000" +
        "0000000000000000000000000000000000000000000000000000000000000000";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, 900_000L, 86_400_000L);
    }

    private UserDetails user(String email) {
        return User.withUsername(email).password("pw").authorities(Collections.emptyList()).build();
    }

    @Test
    void generateAccessToken_AndExtractUsername_ReturnsSameEmail() {
        UserDetails details = user("alice@test.com");
        String token = jwtService.generateAccessToken(details, Map.of());

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@test.com");
    }

    @Test
    void isTokenValid_WithValidToken_ReturnsTrue() {
        UserDetails details = user("alice@test.com");
        String token = jwtService.generateAccessToken(details, Map.of());

        assertThat(jwtService.isTokenValid(token, details)).isTrue();
    }

    @Test
    void isTokenValid_WrongUser_ReturnsFalse() {
        String token = jwtService.generateAccessToken(user("alice@test.com"), Map.of());
        assertThat(jwtService.isTokenValid(token, user("bob@test.com"))).isFalse();
    }

    @Test
    void generateAccessToken_ContainsExpectedClaims() {
        UserDetails details = user("alice@test.com");
        String token = jwtService.generateAccessToken(details, Map.of());

        String subject = jwtService.extractClaim(token, Claims::getSubject);
        java.util.Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        assertThat(subject).isEqualTo("alice@test.com");
        assertThat(expiration).isAfter(new java.util.Date());
    }

    @Test
    void refreshToken_IsDifferentFromAccessToken() {
        UserDetails details = user("alice@test.com");
        String access  = jwtService.generateAccessToken(details, Map.of());
        String refresh = jwtService.generateRefreshToken(details);

        assertThat(access).isNotEqualTo(refresh);
    }
}
