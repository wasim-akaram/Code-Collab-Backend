package com.codesync.authservice.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.codesync.authservice.dto.LoginRequest;
import com.codesync.authservice.dto.RegisterRequest;
import com.codesync.authservice.entity.User;

class DtoEntityTest {

    @Test
    void loginRequest_shouldStoreValues() {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("secret");

        assertEquals("john@example.com", request.getEmail());
        assertEquals("secret", request.getPassword());
    }

    @Test
    void registerRequest_shouldStoreValues() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setUsername("john");
        request.setEmail("john@example.com");
        request.setPassword("secret");

        assertEquals("John Doe", request.getFullName());
        assertEquals("john", request.getUsername());
        assertEquals("john@example.com", request.getEmail());
        assertEquals("secret", request.getPassword());
    }

    @Test
    void userBuilder_shouldBuildEntity() {
        Instant now = Instant.now();

        User user = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .fullName("John Doe")
                .password("encoded")
                .provider("LOCAL")
                .role("DEVELOPER")
                .active(true)
                .createdAt(now)
                .build();

        assertEquals(1L, user.getId());
        assertEquals("john", user.getUsername());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("John Doe", user.getFullName());
        assertEquals("encoded", user.getPassword());
        assertEquals("LOCAL", user.getProvider());
        assertEquals("DEVELOPER", user.getRole());
        assertTrue(user.isActive());
        assertEquals(now, user.getCreatedAt());
    }
}
