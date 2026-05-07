/*
 * Code reader note: Carries request body data from auth-related frontend calls into controller/service methods.
 */
package com.codesync.authservice.dto;

import lombok.Data;

/**
 * Request body for PUT /auth/profile.
 * All fields are optional — only non-null values are applied.
 */
@Data
public class UpdateProfileRequest {
    private String fullName;
    private String username;
    private String bio;
    private String avatarUrl;
}
