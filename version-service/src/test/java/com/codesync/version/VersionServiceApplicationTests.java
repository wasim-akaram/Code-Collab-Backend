package com.codesync.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class VersionServiceApplicationTests {

	@Test
	void shouldCreateApplicationInstance() {
		VersionServiceApplication app = new VersionServiceApplication();
		assertNotNull(app);
	}
}