package com.codesync.authservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.codesync.authservice.dto.ChangePasswordRequest;
import com.codesync.authservice.dto.LoginRequest;
import com.codesync.authservice.dto.RegisterRequest;
import com.codesync.authservice.dto.UpdateProfileRequest;
import com.codesync.authservice.dto.UserProfileDto;
import com.codesync.authservice.entity.User;
import com.codesync.authservice.repository.UserRepository;
import com.codesync.authservice.util.JwtUtil;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock private OtpService otpService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User mockUser;

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

        mockUser = User.builder()
                .email("test@test.com")
                .username("testuser")
                .fullName("Test User")
                .password("hashedPassword")
                .role("DEVELOPER")
                .provider("LOCAL")
                .active(true)
                .createdAt(Instant.now())
                .build();
        mockUser.setId(1L);
    }

    // ─── Register ───────────────────────────────────────────────────────────────

    @Test
    void testRegister_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // otpService.generateOtp is void — no stubbing needed

        String result = authService.register(registerRequest);

        assertEquals("OTP sent to email", result);
        verify(userRepository).existsByEmail("test@test.com");
        verify(userRepository).existsByUsername("testuser");
        verify(otpService).generateOtp("test@test.com");
        verify(valueOperations).set(eq("test@test.com:temp"), eq(registerRequest), any(Duration.class));
    }

    @Test
    void testRegister_EmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));
        assertEquals("Email already exists", ex.getMessage());
        verify(otpService, never()).generateOtp(anyString());
    }

    @Test
    void testRegister_UsernameAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(registerRequest));
        assertEquals("Username already exists", ex.getMessage());
        verify(otpService, never()).generateOtp(anyString());
    }

    @Test
    void testRegister_EmailIsTrimmedAndLowercased() {
        registerRequest.setEmail("  TEST@TEST.COM  ");
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.register(registerRequest);

        verify(userRepository).existsByEmail("test@test.com");
    }

    // ─── FinalRegister ─────────────────────────────────────────────────────────

    @Test
    void testFinalRegister_Success() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User result = authService.finalRegister(registerRequest);

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
        assertEquals("DEVELOPER", result.getRole());
        assertTrue(result.isActive());
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    // ─── Login ──────────────────────────────────────────────────────────────────

    @Test
    void testLogin_Success() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("mockJwtToken");

        String token = authService.login(loginRequest);

        assertEquals("mockJwtToken", token);
        verify(passwordEncoder).matches("password123", "hashedPassword");
        verify(jwtUtil).generateToken("test@test.com", "DEVELOPER");
    }

    @Test
    void testLogin_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void testLogin_DeactivatedAccount() {
        mockUser.setActive(false);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));
        assertEquals("Account is deactivated", ex.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void testLogin_InvalidPassword() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(loginRequest));
        assertEquals("Invalid password", ex.getMessage());
    }

    // ─── GetProfile ─────────────────────────────────────────────────────────────

    @Test
    void testGetProfile_Success() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        UserProfileDto dto = authService.getProfile("test@test.com");

        assertNotNull(dto);
        assertEquals("test@test.com", dto.getEmail());
        assertEquals("testuser", dto.getUsername());
    }

    @Test
    void testGetProfile_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getProfile("missing@test.com"));
    }

    // ─── GetUserById ────────────────────────────────────────────────────────────

    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        UserProfileDto dto = authService.getUserById(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.getUserById(99L));
    }

    // ─── UpdateProfile ──────────────────────────────────────────────────────────

    @Test
    void testUpdateProfile_AllFields() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("New Name");
        req.setUsername("newuser");
        req.setBio("My bio");
        req.setAvatarUrl("http://avatar.url");

        UserProfileDto result = authService.updateProfile("test@test.com", req);

        assertNotNull(result);
        assertEquals("New Name", mockUser.getFullName());
        assertEquals("newuser", mockUser.getUsername());
        assertEquals("My bio", mockUser.getBio());
        assertEquals("http://avatar.url", mockUser.getAvatarUrl());
        verify(userRepository).save(mockUser);
    }

    @Test
    void testUpdateProfile_UsernameTaken() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.existsByUsername("takenuser")).thenReturn(true);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("takenuser");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.updateProfile("test@test.com", req));
        assertEquals("Username already taken", ex.getMessage());
    }

    @Test
    void testUpdateProfile_SameUsernameAllowed() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("testuser"); // same as current

        assertDoesNotThrow(() -> authService.updateProfile("test@test.com", req));
        verify(userRepository, never()).existsByUsername(anyString()); // same name — skip check
    }

    // ─── ChangePassword ─────────────────────────────────────────────────────────

    @Test
    void testChangePassword_Success() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("password123");
        req.setNewPassword("newPassword");

        assertDoesNotThrow(() -> authService.changePassword("test@test.com", req));
        assertEquals("newHash", mockUser.getPassword());
    }

    @Test
    void testChangePassword_WrongOldPassword() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("wrong");
        req.setNewPassword("newPassword");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.changePassword("test@test.com", req));
        assertEquals("Old password is incorrect", ex.getMessage());
    }

    @Test
    void testChangePassword_TooShortNewPassword() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("password123");
        req.setNewPassword("abc"); // < 6 chars

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.changePassword("test@test.com", req));
        assertEquals("New password must be at least 6 characters", ex.getMessage());
    }

    // ─── SearchUsers ────────────────────────────────────────────────────────────

    @Test
    void testSearchUsers_ReturnsResults() {
        when(userRepository.searchByUsername("test")).thenReturn(List.of(mockUser));

        List<UserProfileDto> results = authService.searchUsers("test");

        assertEquals(1, results.size());
        assertEquals("test@test.com", results.get(0).getEmail());
    }

    @Test
    void testSearchUsers_EmptyResult() {
        when(userRepository.searchByUsername("nobody")).thenReturn(List.of());

        List<UserProfileDto> results = authService.searchUsers("nobody");

        assertTrue(results.isEmpty());
    }

    // ─── DeactivateAccount ──────────────────────────────────────────────────────

    @Test
    void testDeactivateAccount_SetsInactive() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        authService.deactivateAccount("test@test.com");

        assertFalse(mockUser.isActive());
        verify(userRepository).save(mockUser);
    }

    @Test
    void testDeactivateAccount_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.deactivateAccount("missing@test.com"));
    }

    // ─── ForgotPassword ─────────────────────────────────────────────────────────

    @Test
    void testForgotPassword_Success() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        assertDoesNotThrow(() -> authService.forgotPassword("test@test.com"));
        verify(otpService).generateOtp("test@test.com");
    }

    @Test
    void testForgotPassword_UserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.forgotPassword("missing@test.com"));
        assertEquals("No account found with this email", ex.getMessage());
    }

    @Test
    void testForgotPassword_DeactivatedAccount() {
        mockUser.setActive(false);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.forgotPassword("test@test.com"));
        assertEquals("Account is deactivated", ex.getMessage());
        verify(otpService, never()).generateOtp(anyString());
    }

    // ─── ResetPassword ──────────────────────────────────────────────────────────

    @Test
    void testResetPassword_Success() {
        when(otpService.verifyOtp("test@test.com", "123456")).thenReturn(true);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("newPass123")).thenReturn("newHash");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        assertDoesNotThrow(() -> authService.resetPassword("test@test.com", "123456", "newPass123"));
        assertEquals("newHash", mockUser.getPassword());
    }

    @Test
    void testResetPassword_InvalidOtp() {
        when(otpService.verifyOtp(anyString(), anyString())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.resetPassword("test@test.com", "wrong", "newPass123"));
        assertEquals("Invalid or expired OTP", ex.getMessage());
    }

    @Test
    void testResetPassword_TooShort() {
        when(otpService.verifyOtp(anyString(), anyString())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.resetPassword("test@test.com", "123456", "abc"));
        assertEquals("Password must be at least 6 characters", ex.getMessage());
    }

    // ─── RefreshToken ───────────────────────────────────────────────────────────

    @Test
    void testRefreshToken_Success() {
        when(jwtUtil.extractEmail("validToken")).thenReturn("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(mockUser));
        when(jwtUtil.generateToken("test@test.com", "DEVELOPER")).thenReturn("newToken");

        String result = authService.refreshToken("Bearer validToken");

        assertEquals("newToken", result);
    }

    @Test
    void testRefreshToken_InvalidFormat() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.refreshToken("not-a-bearer-token"));
        assertEquals("Invalid token format", ex.getMessage());
    }

    @Test
    void testRefreshToken_NullToken() {
        assertThrows(RuntimeException.class, () -> authService.refreshToken(null));
    }

    // ─── LogoutToken ────────────────────────────────────────────────────────────

    @Test
    void testLogoutToken_BlacklistsValidToken() {
        long futureExpiry = System.currentTimeMillis() + 60_000;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.extractExpiry("validToken")).thenReturn(futureExpiry);

        authService.logoutToken("Bearer validToken");

        verify(valueOperations).set(
                eq("blacklist:validToken"),
                eq("revoked"),
                any(Duration.class)
        );
    }

    @Test
    void testLogoutToken_NullTokenIgnored() {
        // Should not throw or interact with Redis
        assertDoesNotThrow(() -> authService.logoutToken(null));
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void testLogoutToken_AlreadyExpiredTokenIgnored() {
        when(jwtUtil.extractExpiry("expiredToken")).thenReturn(System.currentTimeMillis() - 1000);

        authService.logoutToken("Bearer expiredToken");

        // Expired token — Redis should NOT be touched at all
        verifyNoInteractions(valueOperations);
    }

    // ─── Admin Operations ───────────────────────────────────────────────────────

    @Test
    void testGetAllUsers_ReturnsList() {
        when(userRepository.findAll()).thenReturn(List.of(mockUser));

        List<UserProfileDto> result = authService.getAllUsers();

        assertEquals(1, result.size());
        assertEquals("test@test.com", result.get(0).getEmail());
    }

    @Test
    void testSuspendUser_TogglesActiveFlag() {
        assertTrue(mockUser.isActive());
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserProfileDto result = authService.suspendUser(1L);

        assertFalse(mockUser.isActive());
        assertNotNull(result);
        verify(userRepository).save(mockUser);
    }

    @Test
    void testSuspendUser_NotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.suspendUser(99L));
    }

    @Test
    void testDeleteUserById_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> authService.deleteUserById(1L));
        verify(userRepository).deleteById(1L);
    }

    @Test
    void testDeleteUserById_NotFound() {
        when(userRepository.existsById(99L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.deleteUserById(99L));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void testGetAdminStats_ReturnsCorrectCounts() {
        User adminUser = User.builder().role("ADMIN").active(true).email("admin@test.com").build();
        when(userRepository.count()).thenReturn(2L);
        when(userRepository.findAll()).thenReturn(List.of(mockUser, adminUser));
        when(userRepository.findAllByRole("ADMIN")).thenReturn(List.of(adminUser));

        var stats = authService.getAdminStats();

        assertEquals(2L, stats.get("totalUsers"));
        assertEquals(2L, stats.get("activeUsers")); // both active
        assertEquals(1L, stats.get("adminCount"));
    }
}
