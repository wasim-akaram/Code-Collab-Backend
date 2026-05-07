package com.codesync.execution;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExecutionServiceApplicationTests {

	@Test
	void shouldCreateApplicationInstance() {
		ExecutionServiceApplication app = new ExecutionServiceApplication();
		assertNotNull(app);
	}
}