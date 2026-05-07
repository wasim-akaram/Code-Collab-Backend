package com.codesync.notification.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void shouldHandleResourceNotFound() {
		ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(new ResourceNotFoundException("missing"));

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("missing", response.getBody().get("message"));
	}

	@Test
	void shouldHandleSecurityException() {
		ResponseEntity<Map<String, Object>> response = handler.handleSecurityException(new SecurityException("forbidden"));

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("forbidden", response.getBody().get("message"));
	}

	@Test
	void shouldHandleGenericException() {
		ResponseEntity<Map<String, Object>> response = handler.handleGenericException(new RuntimeException("boom"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("boom", response.getBody().get("message"));
	}
}