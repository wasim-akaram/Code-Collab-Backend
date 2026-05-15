package com.codesync.payment.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.ServletException;

/**
 * Unit tests for {@link JwtFilter}.
 * Verifies gateway-header-based authentication and path exclusions.
 */
class JwtFilterTest {

    private final JwtFilter filter = new JwtFilter();

    @Test
    @DisplayName("Request with X-User header should set authentication")
    void doFilter_withXUser_shouldAuthenticate() throws ServletException, IOException {
        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments/subscription");
        request.addHeader("X-User", "user@test.com");
        request.addHeader("X-Role", "DEVELOPER");
        request.addHeader("X-User-Plan", "PRO");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "Authentication should be set");
        assertEquals("user@test.com", auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DEVELOPER")));
    }

    @Test
    @DisplayName("Request without X-User should not set authentication")
    void doFilter_withoutXUser_shouldNotAuthenticate() throws ServletException, IOException {
        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments/plans");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Swagger path should bypass filter entirely")
    void doFilter_swaggerPath_shouldBypass() throws ServletException, IOException {
        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        request.setServletPath("/v3/api-docs");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("X-User without X-Role should default to ROLE_USER")
    void doFilter_noRole_shouldDefaultToRoleUser() throws ServletException, IOException {
        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments/subscription");
        request.addHeader("X-User", "user@test.com");
        // No X-Role header

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("X-User without X-User-Plan should default plan to FREE")
    void doFilter_noPlan_shouldDefaultToFree() throws ServletException, IOException {
        SecurityContextHolder.clearContext();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/payments/subscription");
        request.addHeader("X-User", "user@test.com");
        // No X-User-Plan header

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilterInternal(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        var details = (java.util.Map<?, ?>) auth.getDetails();
        assertEquals("FREE", details.get("plan"));
    }
}
