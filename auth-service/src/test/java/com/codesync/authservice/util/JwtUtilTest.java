package com.codesync.authservice.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil("5h14ouuPuDcEpQlSOrL7zxtiToeBrLhtWPtE1CIIfUM");

    @Test
    void generateAndExtract_shouldReturnOriginalEmail() {
        String token = jwtUtil.generateToken("john@example.com","ADMIN");

        assertNotNull(token);
        assertEquals("john@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    void extractEmail_shouldThrowForInvalidToken() {
        assertThrows(Exception.class, () -> jwtUtil.extractEmail("invalid.token.value"));
    }
}
