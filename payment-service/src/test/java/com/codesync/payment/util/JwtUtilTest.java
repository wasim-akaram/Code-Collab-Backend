package com.codesync.payment.util;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Unit tests for {@link JwtUtil}.
 * Verifies JWT parsing, email extraction, plan extraction, and validation.
 */
class JwtUtilTest {

    // Test secret — must be at least 32 bytes for HS256
    private static final String SECRET = "test-jwt-secret-key-for-unit-testing-only-must-be-long";
    private Key key;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("extractEmail should return subject from valid JWT")
    void extractEmail_validToken_shouldReturnSubject() {
        String token = Jwts.builder()
                .setSubject("user@test.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertEquals("user@test.com", jwtUtil.extractEmail(token));
    }

    @Test
    @DisplayName("extractPlan should return plan claim from JWT")
    void extractPlan_validToken_shouldReturnPlan() {
        String token = Jwts.builder()
                .setSubject("user@test.com")
                .claim("plan", "PRO")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertEquals("PRO", jwtUtil.extractPlan(token));
    }

    @Test
    @DisplayName("extractPlan should default to FREE when plan claim is absent")
    void extractPlan_noPlanClaim_shouldReturnFree() {
        String token = Jwts.builder()
                .setSubject("user@test.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertEquals("FREE", jwtUtil.extractPlan(token));
    }

    @Test
    @DisplayName("isTokenValid should return true for non-expired token")
    void isTokenValid_validToken_shouldReturnTrue() {
        String token = Jwts.builder()
                .setSubject("user@test.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid should return false for expired token")
    void isTokenValid_expiredToken_shouldReturnFalse() {
        String token = Jwts.builder()
                .setSubject("user@test.com")
                .setExpiration(new Date(System.currentTimeMillis() - 60000)) // Expired
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    @DisplayName("isTokenValid should return false for garbage token")
    void isTokenValid_garbageToken_shouldReturnFalse() {
        assertFalse(jwtUtil.isTokenValid("not.a.valid.jwt.token"));
    }

    @Test
    @DisplayName("isTokenValid should return false for wrong key")
    void isTokenValid_wrongKey_shouldReturnFalse() {
        Key wrongKey = Keys.hmacShaKeyFor("different-secret-key-that-is-long-enough-32bytes".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .setSubject("user@test.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(wrongKey, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtUtil.isTokenValid(token));
    }
}
