package com.codesync.authservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Fixed OtpServiceTest.
 *
 * Bugs in original test:
 *  1. generateOtp() stores key as email+":otp", not plain email.
 *  2. verifyOtp() reads key as email+":otp", not plain email.
 *  3. verifyOtp() stores verified flag as String "true", not Boolean true.
 *  4. generateOtp() returns a String (it still returns the OTP so auth service can log it).
 */
@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private EmailService emailService;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private OtpService otpService;

    private static final String EMAIL = "test@test.com";
    private static final String OTP_KEY = EMAIL + ":otp";
    private static final String VERIFIED_KEY = EMAIL + ":verified";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ─── generateOtp ────────────────────────────────────────────────────────────

    @Test
    void testGenerateOtp_StoresWithCorrectKeyAndTtl() {
        // generateOtp is void-like but returns the OTP string for internal use
        String result = otpService.generateOtp(EMAIL);

        assertNotNull(result);
        assertEquals(6, result.length());
        // Key must be "email:otp", not plain email
        verify(valueOperations).set(eq(OTP_KEY), eq(result), eq(Duration.ofMinutes(5)));
    }

    @Test
    void testGenerateOtp_DispatchesEmail() {
        String result = otpService.generateOtp(EMAIL);

        verify(emailService).sendOtp(eq(EMAIL), eq(result));
    }

    @Test
    void testGenerateOtp_ReturnsNumericSixDigitString() {
        String result = otpService.generateOtp(EMAIL);

        assertTrue(result.matches("\\d{6}"), "OTP should be exactly 6 digits");
    }

    // ─── verifyOtp ──────────────────────────────────────────────────────────────

    @Test
    void testVerifyOtp_Success() {
        // Real impl reads from "email:otp", not plain "email"
        when(valueOperations.get(OTP_KEY)).thenReturn("123456");

        boolean result = otpService.verifyOtp(EMAIL, "123456");

        assertTrue(result);
        // Should delete the OTP key (single-use)
        verify(redisTemplate).delete(OTP_KEY);
        // Should set verified flag as String "true"
        verify(valueOperations).set(eq(VERIFIED_KEY), eq("true"), eq(Duration.ofMinutes(10)));
    }

    @Test
    void testVerifyOtp_WrongOtp() {
        when(valueOperations.get(OTP_KEY)).thenReturn("654321");

        boolean result = otpService.verifyOtp(EMAIL, "000000");

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
        verify(valueOperations, never()).set(contains(":verified"), any(), any());
    }

    @Test
    void testVerifyOtp_ExpiredOrNeverGenerated() {
        when(valueOperations.get(OTP_KEY)).thenReturn(null);

        boolean result = otpService.verifyOtp(EMAIL, "123456");

        assertFalse(result);
        verify(redisTemplate, never()).delete(anyString());
        verify(valueOperations, never()).set(contains(":verified"), any(), any());
    }

    @Test
    void testVerifyOtp_DeletesOtpAfterSuccess() {
        when(valueOperations.get(OTP_KEY)).thenReturn("555555");

        otpService.verifyOtp(EMAIL, "555555");

        // OTP must be deleted immediately (single-use)
        verify(redisTemplate).delete(OTP_KEY);
    }

    // ─── isVerified ─────────────────────────────────────────────────────────────

    @Test
    void testIsVerified_ReturnsTrueWhenFlagSet() {
        when(valueOperations.get(VERIFIED_KEY)).thenReturn("true");

        assertTrue(otpService.isVerified(EMAIL));
    }

    @Test
    void testIsVerified_ReturnsFalseWhenFlagAbsent() {
        when(valueOperations.get(VERIFIED_KEY)).thenReturn(null);

        assertFalse(otpService.isVerified(EMAIL));
    }

    @Test
    void testIsVerified_ReturnsFalseWhenFlagIsWrongValue() {
        when(valueOperations.get(VERIFIED_KEY)).thenReturn("false");

        assertFalse(otpService.isVerified(EMAIL));
    }
}
