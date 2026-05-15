/*
 * Code reader note: Contains user identity business logic for registration, OTP
 * finalization, login, password changes, profile updates, token refresh/logout,
 * and admin operations.
 * Annotations used: @Service marks the business layer bean, and
 * @RequiredArgsConstructor injects the final dependencies.
 */
package com.codesync.authservice.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.codesync.authservice.dto.ChangePasswordRequest;
import com.codesync.authservice.dto.LoginRequest;
import com.codesync.authservice.dto.RegisterRequest;
import com.codesync.authservice.dto.UpdateProfileRequest;
import com.codesync.authservice.dto.UserProfileDto;
import com.codesync.authservice.entity.User;
import com.codesync.authservice.repository.UserRepository;
import com.codesync.authservice.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    // ─── REGISTER (Step 1) ──────────────────────────────────────────────────────
    /**
     * Validates uniqueness, generates OTP, temporarily stores request in Redis.
     */
    public String register(RegisterRequest request) {
        // Normalize email once so uniqueness checks, Redis keys, and later login
        // all refer to the same canonical user identity.
        request.setEmail(request.getEmail().toLowerCase().trim());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        otpService.generateOtp(request.getEmail());

        // The real user row is not written yet. The full registration request is
        // cached briefly until the user proves ownership of the email with OTP.
        redisTemplate.opsForValue().set(
            request.getEmail() + ":temp",
            request,
            java.time.Duration.ofMinutes(10)
        );

        return "OTP sent to email";
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email.toLowerCase().trim());
    }

    // ─── FINAL REGISTER (Step 2) ────────────────────────────────────────────────
    /**
     * Called after OTP verification. Persists the user to the database.
     */
    public User finalRegister(RegisterRequest request) {
        // Passwords are stored only as BCrypt hashes. The plain password exists
        // only inside the incoming request object during this method call.
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("LOCAL")
                .role("DEVELOPER")
                .active(true)
                .createdAt(Instant.now())
                .build();

        return userRepository.save(user);
    }

    // ─── LOGIN ──────────────────────────────────────────────────────────────────
    /**
     * Authenticates credentials and returns a JWT token.
     */
    public String login(LoginRequest request) {
        // Login accepts mixed-case input, but the database stores normalized email.
        request.setEmail(request.getEmail().toLowerCase().trim());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole(),
                user.getPlan() != null ? user.getPlan() : "FREE");
    }

    // ─── GET PROFILE ────────────────────────────────────────────────────────────
    /**
     * Returns the profile of the authenticated user (identified by email from JWT).
     */
    public UserProfileDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(user);
    }

    // ─── GET USER BY ID ─────────────────────────────────────────────────────────
    /**
     * Returns the public profile of any user by their numeric ID.
     */
    public UserProfileDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(user);
    }

    // ─── UPDATE PROFILE ─────────────────────────────────────────────────────────
    /**
     * Updates mutable profile fields for the authenticated user.
     * Only non-null fields in the request are applied.
     */
    public UserProfileDto updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // This is a partial update: null fields mean "leave the existing value".
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            // Ensure uniqueness (skip check if it's the same username)
            if (!request.getUsername().equals(user.getUsername())
                    && userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return toDto(userRepository.save(user));
    }

    // ─── CHANGE PASSWORD ────────────────────────────────────────────────────────
    /**
     * Verifies the old password then updates to the new hashed password.
     */
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ─── SEARCH USERS ───────────────────────────────────────────────────────────
    /**
     * Searches active users by username (partial, case-insensitive match).
     */
    public List<UserProfileDto> searchUsers(String query) {
        return userRepository.searchByUsername(query)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ─── DEACTIVATE ACCOUNT ─────────────────────────────────────────────────────
    /**
     * Soft-deletes the authenticated user's account by marking it inactive.
     */
    public void deactivateAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    // ─── FORGOT PASSWORD ──────────────────────────────────────────────────────
    /**
     * Sends a password-reset OTP to the given email, provided the account exists
     * and is active.
     */
    public void forgotPassword(String email) {
        email = email.toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));
        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }
        otpService.generateOtp(email);
    }

    // ─── RESET PASSWORD ─────────────────────────────────────────────────────────
    /**
     * Verifies the OTP and sets a new password for the user.
     */
    public void resetPassword(String email, String otp, String newPassword) {
        email = email.toLowerCase().trim();

        if (!otpService.verifyOtp(email, otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─── REFRESH TOKEN ──────────────────────────────────────────────────────────
    /**
     * Validates an existing JWT and issues a fresh one with a new expiry.
     */
    public String refreshToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid token format");
        }
        String token = bearerToken.substring(7);
        // extractEmail validates signature and expiry; if the old token is bad,
        // no replacement token is issued.
        String email = jwtUtil.extractEmail(token); // throws if invalid/expired
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return jwtUtil.generateToken(user.getEmail(), user.getRole(),
                user.getPlan() != null ? user.getPlan() : "FREE");
    }

    // ─── LOGOUT (server-side token blacklist) ────────────────────────────────────
    /**
     * Blacklists the given JWT in Redis until it naturally expires.
     * After this call, JwtFilter will reject any request carrying this token with 401.
     */
    public void logoutToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return; // nothing to blacklist
        }
        String token = bearerToken.substring(7);
        try {
            long expMs = jwtUtil.extractExpiry(token); // epoch millis
            long ttlMs = expMs - System.currentTimeMillis();
            if (ttlMs > 0) {
                // Store the revoked token only until its original expiry, so Redis
                // does not keep logout entries forever.
                redisTemplate.opsForValue().set(
                    "blacklist:" + token,
                    "revoked",
                    java.time.Duration.ofMillis(ttlMs)
                );
            }
        } catch (Exception e) {
            // Token already expired/invalid — no need to blacklist
        }
    }

    // ─── MAPPER ─────────────────────────────────────────────────────────────────
    private UserProfileDto toDto(User u) {
        // Keep API responses free of sensitive fields such as password hashes.
        return UserProfileDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .role(u.getRole())
                .bio(u.getBio())
                .avatarUrl(u.getAvatarUrl())
                .provider(u.getProvider())
                .active(u.isActive())
                .createdAt(u.getCreatedAt())
                .plan(u.getPlan() != null ? u.getPlan() : "FREE")
                .planExpiresAt(u.getPlanExpiresAt())
                .build();
    }

    // ─── ADMIN OPERATIONS ───────────────────────────────────────────────────────

    /** Returns all users (admin only). */
    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Toggles the active flag of a user (suspend/unsuspend). */
    public UserProfileDto suspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(!user.isActive());
        return toDto(userRepository.save(user));
    }

    /** Hard-deletes a user by ID (admin only). */
    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    /** Returns platform-wide user stats for the admin dashboard. */
    public java.util.Map<String, Long> getAdminStats() {
        var allUsers = userRepository.findAll();
        long total      = allUsers.size();
        long active     = allUsers.stream().filter(User::isActive).count();
        long suspended  = total - active;
        long admins     = allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
        long proUsers   = allUsers.stream().filter(u -> "PRO".equals(u.getPlan())).count();
        return java.util.Map.of(
                "totalUsers",     total,
                "activeUsers",    active,
                "suspendedUsers", suspended,
                "adminCount",     admins,
                "proUsers",       proUsers
        );
    }

    // ─── PLAN UPDATE (called by payment-service) ────────────────────────────────

    /**
     * Updates the user's subscription plan. Called internally by the payment-service
     * after successful payment verification.
     */
    public UserProfileDto updatePlan(String email, String plan, Instant expiresAt) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPlan(plan);
        user.setPlanExpiresAt(expiresAt);
        return toDto(userRepository.save(user));
    }
}
