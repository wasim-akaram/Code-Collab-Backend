package com.codesync.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiGatewayApplicationTests {

	@Test
	void shouldCreateApplicationInstance() {
		ApiGatewayApplication app = new ApiGatewayApplication();
		assertNotNull(app);
	}
}