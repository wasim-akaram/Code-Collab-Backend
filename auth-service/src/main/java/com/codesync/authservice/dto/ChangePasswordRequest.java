/*
 * Code reader note: Carries request body data from auth-related frontend calls into
 * controller and service methods.
 * Annotations used: @Data generates accessors and boilerplate for the password-change payload.
 */
package com.codesync.authservice.dto;

import lombok.Data;

/**
 * Request body for PUT /auth/password.
 */
@Data
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
}
