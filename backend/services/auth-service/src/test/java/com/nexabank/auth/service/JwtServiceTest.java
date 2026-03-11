package com.nexabank.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        // 512-bit base64 test secret
        ReflectionTestUtils.setField(jwtService, "secretKey",
            "dGVzdFNlY3JldEtleUZvclVuaXRUZXN0aW5nT25seU5vdEZvclByb2R1Y3Rpb25Vc2UxMjM0NTY=");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiry",  900_000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiry", 86_400_000L);
    }

    private UserDetails user(String email) {
        return User.withUsername(email).password("pw").authorities(Collections.emptyList()).build();
    }

    @Test
    void generateToken_AndExtractUsername_ReturnsSameEmail() {
        UserDetails details = user("alice@test.com");
        String token = jwtService.generateToken(details);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@test.com");
    }

    @Test
    void isTokenValid_WithValidToken_ReturnsTrue() {
        UserDetails details = user("alice@test.com");
        String token = jwtService.generateToken(details);

        assertThat(jwtService.isTokenValid(token, details)).isTrue();
    }

    @Test
    void isTokenValid_WrongUser_ReturnsFalse() {
        String token = jwtService.generateToken(user("alice@test.com"));
        assertThat(jwtService.isTokenValid(token, user("bob@test.com"))).isFalse();
    }

    @Test
    void generateToken_ContainsExpectedClaims() {
        UserDetails details = user("alice@test.com");
        String token = jwtService.generateToken(details);
        Claims claims = jwtService.extractAllClaims(token);

        assertThat(claims.getSubject()).isEqualTo("alice@test.com");
        assertThat(claims.getExpiration()).isAfter(new java.util.Date());
    }

    @Test
    void refreshToken_IsDifferentFromAccessToken() {
        UserDetails details = user("alice@test.com");
        String access  = jwtService.generateToken(details);
        String refresh = jwtService.generateRefreshToken(details);

        assertThat(access).isNotEqualTo(refresh);
    }
}
