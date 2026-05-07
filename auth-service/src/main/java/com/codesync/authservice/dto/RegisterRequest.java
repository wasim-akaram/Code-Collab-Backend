/*
 * Code reader note: Carries request body data from auth-related frontend calls into controller/service methods.
 */
package com.codesync.authservice.dto;

import java.io.Serializable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class RegisterRequest implements Serializable 
{
    private static final long serialVersionUID = 1L;

    // Full display name of the user.
    @NotBlank
    private String fullName;

    // Required username.
    @NotBlank
    private String username;

    // Email must be valid format.
    @Email
    @NotBlank
    private String email;

    // Required raw password.
    @NotBlank
    private String password;
  
}