/*
 * Code reader note: Carries request body data from auth-related frontend calls into
 * controller and service methods.
 * Annotations used: @Data generates accessors and boilerplate for the login payload.
 */
package com.codesync.authservice.dto;

import lombok.Data;

@Data
public class LoginRequest {
    // Email used for authentication.
    private String email;
    // Raw password provided during login.
    private String password;
}