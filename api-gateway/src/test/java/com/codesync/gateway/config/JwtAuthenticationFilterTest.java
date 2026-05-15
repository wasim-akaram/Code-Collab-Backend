package com.codesync.gateway.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

	private JwtAuthenticationFilter filter;
	private GatewayFilterChain chain;

	@BeforeEach
	void setUp() {
		filter = new JwtAuthenticationFilter();
		ReflectionTestUtils.setField(filter, "secret", "01234567890123456789012345678901");
		chain = mock(GatewayFilterChain.class);
		when(chain.filter(any())).thenReturn(reactor.core.publisher.Mono.empty());
	}

	@Test
	void shouldAllowSwaggerPathsWithoutToken() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/auth/login").build());

		filter.filter(exchange, chain).block();

		verify(chain).filter(exchange);
	}

	@Test
	void shouldRejectMissingToken() {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/projects/1").build());

		filter.filter(exchange, chain).block();

		assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
	}

	@Test
	void shouldForwardUserHeaderForValidToken() {
		String secret = "01234567890123456789012345678901";
		String token = Jwts.builder()
				.setSubject("42")
				.setIssuedAt(new Date())
				.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
				.compact();

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/projects/1")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.build()
		);

		filter.filter(exchange, chain).block();

		var captor = forClass(ServerWebExchange.class);
		verify(chain).filter(captor.capture());
		assertNotNull(captor.getValue().getRequest().getHeaders().getFirst("X-User"));
		assertEquals("42", captor.getValue().getRequest().getHeaders().getFirst("X-User"));
	}
}