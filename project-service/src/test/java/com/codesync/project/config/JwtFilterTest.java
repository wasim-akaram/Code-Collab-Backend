package com.codesync.project.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for {@link JwtFilter}. */
class JwtFilterTest {

    private final JwtFilter filter = new JwtFilter();

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test @DisplayName("X-User header sets authentication")
    void withXUser() throws Exception {
        var req = new MockHttpServletRequest("GET", "/projects/my");
        req.addHeader("X-User", "u@t.com");
        req.addHeader("X-Role", "DEVELOPER");
        req.addHeader("X-User-Plan", "PRO");
        filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("u@t.com", auth.getPrincipal());
    }

    @Test @DisplayName("No X-User leaves context empty")
    void withoutXUser() throws Exception {
        var req = new MockHttpServletRequest("GET", "/projects/public");
        filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test @DisplayName("Swagger path bypasses filter")
    void swaggerPath() throws Exception {
        var req = new MockHttpServletRequest("GET", "/v3/api-docs");
        req.setServletPath("/v3/api-docs");
        filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test @DisplayName("Null servlet path uses requestURI")
    void nullServletPath() throws Exception {
        var req = new MockHttpServletRequest("GET", "/projects/1");
        req.addHeader("X-User", "u@t.com");
        filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test @DisplayName("Missing role gives empty authorities")
    void noRole() throws Exception {
        var req = new MockHttpServletRequest("GET", "/projects/1");
        req.addHeader("X-User", "u@t.com");
        filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth.getAuthorities().isEmpty());
    }

    @Test @DisplayName("Missing plan defaults to FREE")
    void noPlan() throws Exception {
        var req = new MockHttpServletRequest("GET", "/projects/1");
        req.addHeader("X-User", "u@t.com");
        filter.doFilterInternal(req, new MockHttpServletResponse(), new MockFilterChain());
        var details = (java.util.Map<?, ?>) SecurityContextHolder.getContext().getAuthentication().getDetails();
        assertEquals("FREE", details.get("plan"));
    }
}
