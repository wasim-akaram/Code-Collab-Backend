package com.codesync.authservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.codesync.authservice.dto.LoginRequest;
import com.codesync.authservice.dto.RegisterRequest;
import com.codesync.authservice.entity.User;
import com.codesync.authservice.service.AuthService;
import com.codesync.authservice.service.OtpService;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private OtpService otpService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthController authController;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("Test User");
        registerRequest.setEmail("test@test.com");
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@test.com");
        loginRequest.setPassword("password123");
    }

    @Test
    void testRegister_Success() {
        when(authService.register(any(RegisterRequest.class))).thenReturn("OTP sent to email");

        ResponseEntity<String> response = authController.register(registerRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OTP sent to email", response.getBody());
        verify(authService).register(registerRequest);
    }

    @Test
    void testVerifyAndRegister_Success() {
        String email = "test@test.com";
        String otp = "123456";
        
        when(otpService.verifyOtp(email, otp)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(email + ":temp")).thenReturn(registerRequest);
        when(authService.finalRegister(any(RegisterRequest.class))).thenReturn(new User());

        ResponseEntity<String> response = authController.verifyAndRegister(email, otp);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User registered successfully", response.getBody());
        verify(authService).finalRegister(registerRequest);
    }

    @Test
    void testVerifyAndRegister_InvalidOtp() {
        String email = "test@test.com";
        String otp = "123456";
        
        when(otpService.verifyOtp(email, otp)).thenReturn(false);

        ResponseEntity<String> response = authController.verifyAndRegister(email, otp);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid OTP", response.getBody());
        verify(authService, never()).finalRegister(any());
    }

    @Test
    void testVerifyAndRegister_SessionExpired() {
        String email = "test@test.com";
        String otp = "123456";
        
        when(otpService.verifyOtp(email, otp)).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(email + ":temp")).thenReturn(null);

        ResponseEntity<String> response = authController.verifyAndRegister(email, otp);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Session expired. Please register again.", response.getBody());
        verify(authService, never()).finalRegister(any());
    }

    @Test
    void testLogin_Success() {
        when(authService.login(any(LoginRequest.class))).thenReturn("mockJwtToken");

        ResponseEntity<String> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("mockJwtToken", response.getBody());
        verify(authService).login(loginRequest);
    }
}
