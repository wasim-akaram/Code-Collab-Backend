/*
 * Code reader note: Exposes direct OTP send and verify endpoints backed by the OTP service.
 * Annotations used: @RestController publishes the REST API, @RequestMapping sets the
 * /auth base path, @RequiredArgsConstructor injects the dependencies, and @PostMapping
 * maps the OTP endpoints.
 */
package com.codesync.authservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.codesync.authservice.repository.UserRepository;
import com.codesync.authservice.service.OtpService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OtpController {

    // Handles OTP generation and verification.
    private final OtpService otpService;
    // Used to block OTP for already registered emails.
    private final UserRepository userRepository;

    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestParam String email) {

        // Normalize email for consistent Redis key storage
        String normalizedEmail = email.toLowerCase().trim();

        // Do not send OTP for users that are already registered.
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        // Generate and send OTP for new email.
        otpService.generateOtp(normalizedEmail);

        return ResponseEntity.ok("OTP sent");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestParam String email,
                                            @RequestParam String otp) {

        // Normalize email for consistent Redis key lookup
        String normalizedEmail = email.toLowerCase().trim();

        // Return success only when provided OTP matches Redis value.
        if (otpService.verifyOtp(normalizedEmail, otp)) {
            return ResponseEntity.ok("OTP verified");
        }

        // Invalid or expired OTP.
        return ResponseEntity.badRequest().body("Invalid OTP");
    }
}