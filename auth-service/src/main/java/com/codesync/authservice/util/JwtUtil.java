/*
 * Code reader note: Creates and parses signed JWT tokens that carry the user email,
 * role, and expiry.
 * Annotations used: @Component registers the utility as a Spring bean, and
 * @Value injects the JWT secret from configuration.
 */
package com.codesync.authservice.util;

import java.security.Key;


import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final Key key;
    private final long EXPIRATION = 1000 * 60 * 60;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
    	
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      
    }

    public String generateToken(String email, String role) {
        return generateToken(email, role, "FREE");
    }

    public String generateToken(String email, String role, String plan) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("plan", plan != null ? plan : "FREE")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key, io.jsonwebtoken.SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /** Extracts the {@code role} custom claim from a signed JWT. */
    public String extractRole(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("role", String.class);
    }

    /** Extracts the {@code plan} custom claim from a signed JWT. */
    public String extractPlan(String token) {
        String plan = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("plan", String.class);
        return plan != null ? plan : "FREE";
    }

    /** Returns the expiration time of the token as epoch milliseconds. */
    public long extractExpiry(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration()
                .getTime();
    }
}