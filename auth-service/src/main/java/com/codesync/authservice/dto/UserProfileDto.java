/*
 * Code reader note: Carries user profile data out through auth-service API responses.
 */
package com.codesync.authservice.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO returned by GET /auth/profile and GET /auth/users/{id}.
 * Never exposes passwordHash or sensitive internal fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String role;
    private String bio;
    private String avatarUrl;
    private String provider;
    private boolean active;
    private Instant createdAt;
}
