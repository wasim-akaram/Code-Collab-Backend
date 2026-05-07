package com.codesync.project.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the gateway-trusting JwtFilter.
 * The filter reads the X-User header set by the API Gateway
 * and does NOT validate JWT tokens itself.
 */
@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtFilter = new JwtFilter();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_shouldAuthenticateWhenXUserHeaderIsPresent() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/projects");
        when(request.getHeader("X-User")).thenReturn("test@example.com");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("test@example.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().isEmpty());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticate_whenXUserHeaderIsMissing() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/projects");
        when(request.getHeader("X-User")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotAuthenticate_whenXUserHeaderIsEmpty() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/projects");
        when(request.getHeader("X-User")).thenReturn("");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldSkipAuth_forSwaggerApiDocs() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/v3/api-docs");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        // Should NOT attempt to read the X-User header for swagger paths
        verify(request, never()).getHeader("X-User");
    }

    @Test
    void doFilterInternal_shouldSkipAuth_forSwaggerUi() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/swagger-ui/index.html");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(request, never()).getHeader("X-User");
    }

    @Test
    void doFilterInternal_shouldSkipAuth_forSwaggerApiDocsSubpath() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/v3/api-docs/swagger-config");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(request, never()).getHeader("X-User");
    }

    @Test
    void doFilterInternal_shouldFallbackToRequestURI_whenServletPathIsNull() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/projects");
        when(request.getHeader("X-User")).thenReturn("user@test.com");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user@test.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldHandleNullServletPathAndNullRequestURI() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn(null);
        when(request.getRequestURI()).thenReturn(null);
        when(request.getHeader("X-User")).thenReturn("user@test.com");

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldAlwaysContinueFilterChain_forNonSwaggerPaths() throws ServletException, IOException {
        when(request.getServletPath()).thenReturn("/projects/1");
        when(request.getHeader("X-User")).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        // Even without X-User header, the filter chain should continue
        // (security is handled by Spring Security's authorization, not this filter)
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
