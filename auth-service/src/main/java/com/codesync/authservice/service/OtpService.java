/*
 * Code reader note: Generates, stores, verifies, and tracks OTP codes in Redis for signup and password reset flows.
 */
package com.codesync.authservice.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;

    /** OTP validity window in minutes. */
    private static final long OTP_EXPIRATION_MINUTES = 5;

    /** Verified-flag TTL — enough time to complete registration after verifying. */
    private static final long VERIFIED_FLAG_MINUTES  = 10;

    /**
     * Generates a cryptographically-seeded 6-digit OTP, stores it in Redis
     * with a 5-minute TTL, and dispatches it via async email.
     */
    public String generateOtp(String email) {
        // SecureRandom for better randomness than Math.random()
        java.security.SecureRandom rng = new java.security.SecureRandom();
        String otp = String.format("%06d", rng.nextInt(1_000_000));

        // Store with expiry — overwrites any previous OTP for this email
        redisTemplate.opsForValue().set(email + ":otp", otp, Duration.ofMinutes(OTP_EXPIRATION_MINUTES));

        log.debug("OTP generated for {} — dispatching async email", email);
        emailService.sendOtp(email, otp);

        return otp;
    }

    /**
     * Verifies the submitted OTP against the Redis-stored value.
     * On success: deletes the OTP key (single-use) and sets a verified flag.
     * On failure: leaves OTP in Redis so the user can retry within the TTL.
     */
    public boolean verifyOtp(String email, String submittedOtp) {
        Object stored = redisTemplate.opsForValue().get(email + ":otp");

        if (stored == null) {
            log.warn("OTP verification failed for {} — no OTP found (expired or never generated)", email);
            return false;
        }

        if (stored.toString().equals(submittedOtp)) {
            // Single-use: delete immediately so the code can't be reused
            redisTemplate.delete(email + ":otp");
            // Set verified flag so verifyAndRegister / resetPassword can trust it
            redisTemplate.opsForValue().set(email + ":verified", "true",
                                            Duration.ofMinutes(VERIFIED_FLAG_MINUTES));
            log.info("OTP verified for {}", email);
            return true;
        }

        log.warn("OTP mismatch for {} — submitted: {}", email, submittedOtp);
        return false;
    }

    /**
     * Checks whether the verified flag is set for an email (used by
     * verifyAndRegister to confirm the OTP step was completed).
     */
    public boolean isVerified(String email) {
        return Boolean.TRUE.toString().equals(
            String.valueOf(redisTemplate.opsForValue().get(email + ":verified"))
        );
    }
}