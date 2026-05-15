package com.codesync.auth_service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.codesync.authservice.AuthServiceApplication;

class AuthServiceApplicationTests {

	@Test
	void shouldCreateApplicationInstance() {
		AuthServiceApplication app = new AuthServiceApplication();
		assertNotNull(app);
	}
}
