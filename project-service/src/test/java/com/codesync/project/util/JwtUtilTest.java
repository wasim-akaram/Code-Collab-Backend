package com.codesync.project.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "5h14ouuPuDcEpQlSOrL7zxtiToeBrLhtWPtE1CIIfUM";
    private Key key;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret);
        key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    private String createValidToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private String createExpiredToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 2))
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = createValidToken("test@example.com");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid.token.string"));
    }

    @Test
    void validateToken_shouldReturnFalseForExpiredToken() {
        String token = createExpiredToken("test@example.com");
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        String token = createValidToken("john.doe@example.com");
        assertEquals("john.doe@example.com", jwtUtil.extractEmail(token));
    }
}
