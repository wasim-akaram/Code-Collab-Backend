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

    @Test
    void testHealth() {
        ResponseEntity<String> response = authController.health();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Auth service is UP", response.getBody());
    }

    @Test
    void testEmailExists() {
        when(authService.emailExists("test@test.com")).thenReturn(true);
        ResponseEntity<Boolean> response = authController.emailExists("test@test.com");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(true, response.getBody());
    }

    @Test
    void testForgotPassword() {
        ResponseEntity<String> response = authController.forgotPassword(java.util.Map.of("email", "test@test.com"));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password reset OTP sent to email", response.getBody());
        verify(authService).forgotPassword("test@test.com");
    }

    @Test
    void testResetPassword() {
        com.codesync.authservice.dto.ResetPasswordRequest req = new com.codesync.authservice.dto.ResetPasswordRequest();
        req.setEmail("test@test.com");
        req.setOtp("123456");
        req.setNewPassword("newpass");
        ResponseEntity<String> response = authController.resetPassword(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password reset successfully", response.getBody());
        verify(authService).resetPassword("test@test.com", "123456", "newpass");
    }

    @Test
    void testRefresh() {
        when(authService.refreshToken("Bearer token")).thenReturn("newToken");
        ResponseEntity<String> response = authController.refresh("Bearer token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("newToken", response.getBody());
    }

    @Test
    void testLogout() {
        ResponseEntity<String> response = authController.logout("Bearer token");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Logged out successfully", response.getBody());
        verify(authService).logoutToken("Bearer token");
    }

    @Test
    void testGetProfile() {
        com.codesync.authservice.dto.UserProfileDto dto = new com.codesync.authservice.dto.UserProfileDto();
        when(authService.getProfile("test@test.com")).thenReturn(dto);
        ResponseEntity<com.codesync.authservice.dto.UserProfileDto> response = authController.getProfile("test@test.com");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testUpdateProfile() {
        com.codesync.authservice.dto.UpdateProfileRequest req = new com.codesync.authservice.dto.UpdateProfileRequest();
        com.codesync.authservice.dto.UserProfileDto dto = new com.codesync.authservice.dto.UserProfileDto();
        when(authService.updateProfile("test@test.com", req)).thenReturn(dto);
        ResponseEntity<com.codesync.authservice.dto.UserProfileDto> response = authController.updateProfile("test@test.com", req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(dto, response.getBody());
    }

    @Test
    void testChangePassword() {
        com.codesync.authservice.dto.ChangePasswordRequest req = new com.codesync.authservice.dto.ChangePasswordRequest();
        ResponseEntity<String> response = authController.changePassword("test@test.com", req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Password changed successfully", response.getBody());
    }

    @Test
    void testSearchUsers() {
        ResponseEntity<java.util.List<com.codesync.authservice.dto.UserProfileDto>> response = authController.searchUsers("test");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).searchUsers("test");
    }

    @Test
    void testGetUserById() {
        ResponseEntity<com.codesync.authservice.dto.UserProfileDto> response = authController.getUserById(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).getUserById(1L);
    }

    @Test
    void testDeactivate() {
        ResponseEntity<String> response = authController.deactivate("test@test.com");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Account deactivated successfully", response.getBody());
    }

    @Test
    void testGetAllUsers() {
        ResponseEntity<java.util.List<com.codesync.authservice.dto.UserProfileDto>> response = authController.getAllUsers();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).getAllUsers();
    }

    @Test
    void testSuspendUser() {
        ResponseEntity<com.codesync.authservice.dto.UserProfileDto> response = authController.suspendUser(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).suspendUser(1L);
    }

    @Test
    void testDeleteUser() {
        ResponseEntity<String> response = authController.deleteUser(1L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("User deleted", response.getBody());
    }

    @Test
    void testGetAdminStats() {
        ResponseEntity<java.util.Map<String, Long>> response = authController.getAdminStats();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).getAdminStats();
    }

    @Test
    void testUpdatePlan() {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("email", "test@test.com");
        body.put("plan", "PRO");
        ResponseEntity<com.codesync.authservice.dto.UserProfileDto> response = authController.updatePlan(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).updatePlan(eq("test@test.com"), eq("PRO"), any());
    }
}
