/*
 * Code reader note: Carries request body data from auth-related frontend calls into
 * controller and service methods.
 * Annotations used: @Data generates accessors and boilerplate for the reset-password payload.
 */
package com.codesync.authservice.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String otp;
    private String newPassword;
}
