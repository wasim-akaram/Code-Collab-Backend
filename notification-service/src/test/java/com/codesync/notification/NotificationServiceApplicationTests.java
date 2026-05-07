package com.codesync.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class NotificationServiceApplicationTests {

	@Test
	void shouldCreateApplicationInstance() {
		NotificationServiceApplication app = new NotificationServiceApplication();
		assertNotNull(app);
	}
}