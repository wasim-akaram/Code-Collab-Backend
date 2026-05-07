/*
 * Code reader note: Parses and validates JWTs used by project-service security.
 */
package com.codesync.project.util;

import io.jsonwebtoken.*;

import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;

/**
 * Utility class for parsing and validating JWT tokens.
 */
@Slf4j
@Component
public class JwtUtil {

    private final Key key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
       
    }

    /**
     * Extracts the user email from the given JWT token.
     * @param token The JWT token.
     * @return The user's email.
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Validates the given JWT token.
     * @param token The JWT token.
     * @return true if valid, false otherwise.
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage(), e);
            
            // these two lines will tell us why JWT authentication fails and prints error message
            System.out.println("JWT ERROR = " + e.getClass().getSimpleName());
            System.out.println("MESSAGE = " + e.getMessage());
            //return false;
            return false;
        }
    }

    /**
     * Parses the JWT claims.
     * @param token The JWT token.
     * @return The extracted claims.
     */
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    
}