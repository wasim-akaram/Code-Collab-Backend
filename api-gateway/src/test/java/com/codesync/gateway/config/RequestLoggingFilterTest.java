package com.codesync.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestLoggingFilterTest {

	@Test
	void shouldAddRequestIdAndContinue() {
		RequestLoggingFilter filter = new RequestLoggingFilter();
		GatewayFilterChain chain = mock(GatewayFilterChain.class);
		when(chain.filter(any())).thenReturn(reactor.core.publisher.Mono.empty());

		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/projects/1")
					.header("Authorization", "Bearer token")
					.build()
		);

		filter.apply(new RequestLoggingFilter.Config()).filter(exchange, chain).block();

		var captor = forClass(ServerWebExchange.class);
		verify(chain).filter(captor.capture());
		assertNotNull(captor.getValue().getRequest().getHeaders().getFirst("X-Request-ID"));
		assertNotNull(exchange.getAttributes().get("gateway.request.start.time"));
		assertEquals("Bearer token", captor.getValue().getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
	}
}