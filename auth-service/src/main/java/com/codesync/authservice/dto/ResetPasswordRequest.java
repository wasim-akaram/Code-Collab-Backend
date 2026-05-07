/*
 * Code reader note: Carries request body data from auth-related frontend calls into controller/service methods.
 */
package com.codesync.authservice.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String otp;
    private String newPassword;
}
