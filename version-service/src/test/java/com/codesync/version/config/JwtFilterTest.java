package com.codesync.version.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JwtFilterTest {

	private JwtFilter jwtFilter;
	private FilterChain filterChain;

	@BeforeEach
	void setUp() {
		jwtFilter = new JwtFilter();
		filterChain = mock(FilterChain.class);
		SecurityContextHolder.clearContext();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void shouldAuthenticateWhenHeaderExists() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-User", "42");

		jwtFilter.doFilter(request, new MockHttpServletResponse(), filterChain);

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		assertNotNull(authentication);
		assertEquals("42", authentication.getPrincipal());
		verify(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
	}

	@Test
	void shouldContinueWithoutAuthenticationWhenHeaderMissing() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();

		jwtFilter.doFilter(request, new MockHttpServletResponse(), filterChain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(filterChain).doFilter(any(ServletRequest.class), any(ServletResponse.class));
	}
}