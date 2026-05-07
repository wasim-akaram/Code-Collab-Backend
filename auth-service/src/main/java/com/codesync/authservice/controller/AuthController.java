/*
 * Code reader note: Exposes REST endpoints for registration, OTP verification, login, profile management, password flows, search, and admin user actions.
 */
package com.codesync.authservice.controller;

import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.codesync.authservice.dto.ChangePasswordRequest;
import com.codesync.authservice.dto.LoginRequest;
import com.codesync.authservice.dto.RegisterRequest;
import com.codesync.authservice.dto.ResetPasswordRequest;
import com.codesync.authservice.dto.UpdateProfileRequest;
import com.codesync.authservice.dto.UserProfileDto;
import com.codesync.authservice.service.AuthService;
import com.codesync.authservice.service.OtpService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for all /auth/** endpoints.
 *
 * Public endpoints  (no JWT):  register, verify-and-register, login,
 *                               send-otp, verify-otp, refresh, search,
 *                               users/{id}, health, forgot-password,
 *                               reset-password
 * Protected endpoints (JWT):   profile (GET/PUT), password (PUT),
 *                               deactivate (DELETE)
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final RedisTemplate<String, Object> redisTemplate;

    // ─── HEALTH CHECK ──────────────────────────────────────────────────────────

    /** Simple liveness probe used by the API Gateway and monitoring tools. */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth service is UP");
    }

    // ─── REGISTRATION ──────────────────────────────────────────────────────────

    /**
     * Step 1: Validate details and send OTP to email.
     * POST /auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @GetMapping("/email-exists")
    public ResponseEntity<Boolean> emailExists(@RequestParam String email) {
        return ResponseEntity.ok(authService.emailExists(email));
    }

    /**
     * Step 2: Verify OTP → finalize registration → save to DB.
     * POST /auth/verify-and-register?email=&otp=
     */
    @PostMapping("/verify-and-register")
    public ResponseEntity<String> verifyAndRegister(@RequestParam String email,
                                                    @RequestParam String otp) {
        String normalizedEmail = email.toLowerCase().trim();

        if (!otpService.verifyOtp(normalizedEmail, otp)) {
            return ResponseEntity.badRequest().body("Invalid OTP");
        }

        RegisterRequest request =
                (RegisterRequest) redisTemplate.opsForValue().get(normalizedEmail + ":temp");

        if (request == null) {
            return ResponseEntity.badRequest().body("Session expired. Please register again.");
        }

        authService.finalRegister(request);
        return ResponseEntity.ok("User registered successfully");
    }

    // ─── LOGIN ─────────────────────────────────────────────────────────────────

    /**
     * Authenticate with email + password → returns JWT token.
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // ─── FORGOT PASSWORD ──────────────────────────────────────────────────────

    /**
     * Step 1: Send a password-reset OTP to the user's email.
     * POST /auth/forgot-password  (public — no JWT)
     * Body: { "email": "..." }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody java.util.Map<String, String> body) {
        String email = body.getOrDefault("email", "");
        authService.forgotPassword(email);
        return ResponseEntity.ok("Password reset OTP sent to email");
    }

    /**
     * Step 2: Verify OTP + set new password.
     * POST /auth/reset-password  (public — no JWT)
     * Body: { "email": "...", "otp": "...", "newPassword": "..." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok("Password reset successfully");
    }

    // ─── TOKEN REFRESH ─────────────────────────────────────────────────────────

    /**
     * Exchange a still-valid JWT for a fresh one with renewed expiry.
     * POST /auth/refresh  (Authorization: Bearer <token>)
     */
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return ResponseEntity.ok(authService.refreshToken(authHeader));
    }

    // ─── LOGOUT ────────────────────────────────────────────────────────────────

    /**
     * Invalidate the current JWT server-side by blacklisting it in Redis.
     * POST /auth/logout  (Authorization: Bearer <token>)
     * The token stays blacklisted until its natural expiry — then Redis auto-removes it.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logoutToken(authHeader);
        return ResponseEntity.ok("Logged out successfully");
    }

    // ─── PROFILE ───────────────────────────────────────────────────────────────

    /**
     * Return the authenticated user's own profile.
     * GET /auth/profile  (JWT required)
     */
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(authService.getProfile(email));
    }

    /**
     * Update the authenticated user's mutable profile fields.
     * PUT /auth/profile  (JWT required)
     */
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal String email,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(email, request));
    }

    // ─── PASSWORD ──────────────────────────────────────────────────────────────

    /**
     * Change the authenticated user's password.
     * PUT /auth/password  (JWT required)
     * Body: { "oldPassword": "...", "newPassword": "..." }
     */
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal String email,
            @RequestBody ChangePasswordRequest request) {
        authService.changePassword(email, request);
        return ResponseEntity.ok("Password changed successfully");
    }

    // ─── USER LOOKUP ───────────────────────────────────────────────────────────

    /**
     * Search users by username (public — guests can search developers).
     * GET /auth/search?q=john
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserProfileDto>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(authService.searchUsers(q));
    }

    /**
     * Get a user's public profile by their numeric ID.
     * GET /auth/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    // ─── ACCOUNT DEACTIVATION ──────────────────────────────────────────────────

    /**
     * Soft-deactivate the authenticated user's own account.
     * DELETE /auth/deactivate  (JWT required)
     */
    @DeleteMapping("/deactivate")
    public ResponseEntity<String> deactivate(@AuthenticationPrincipal String email) {
        authService.deactivateAccount(email);
        return ResponseEntity.ok("Account deactivated successfully");
    }

    // ─── Admin-only endpoints ─────────────────────────────────────────────────

    /** Get all users — ADMIN only. */
    @GetMapping("/admin/users")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.List<com.codesync.authservice.dto.UserProfileDto>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /** Toggle active/suspended status of a user — ADMIN only. */
    @PutMapping("/admin/users/{id}/suspend")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.codesync.authservice.dto.UserProfileDto> suspendUser(@PathVariable Long id) {
        return ResponseEntity.ok(authService.suspendUser(id));
    }

    /** Hard-delete a user by ID — ADMIN only. */
    @DeleteMapping("/admin/users/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        authService.deleteUserById(id);
        return ResponseEntity.ok("User deleted");
    }

    /** Platform-wide user stats — ADMIN only. */
    @GetMapping("/admin/stats")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String, Long>> getAdminStats() {
        return ResponseEntity.ok(authService.getAdminStats());
    }
}
