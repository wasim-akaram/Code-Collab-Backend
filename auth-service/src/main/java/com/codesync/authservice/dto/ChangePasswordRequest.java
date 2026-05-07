/*
 * Code reader note: Carries request body data from auth-related frontend calls into controller/service methods.
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
