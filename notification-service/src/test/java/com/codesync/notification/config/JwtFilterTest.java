package com.codesync.notification.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtFilterTest {

    private final JwtFilter filter = new JwtFilter();

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test @DisplayName("X-User header sets authentication")
    void withXUser() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/notifications");
        req.addHeader("X-User", "u@t.com");
        req.addHeader("X-Role", "DEVELOPER");
        filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("u@t.com", auth.getPrincipal());
    }

    @Test @DisplayName("No X-User leaves context empty")
    void withoutXUser() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/notifications");
        filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test @DisplayName("Missing role gives empty authorities")
    void noRole() throws Exception {
        var req = new MockHttpServletRequest("GET", "/api/v1/notifications");
        req.addHeader("X-User", "u@t.com");
        filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth.getAuthorities().isEmpty());
    }
}
