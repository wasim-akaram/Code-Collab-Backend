package com.codesync.comment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommentServiceApplicationTests {

	@Test
	void shouldCreateApplicationInstance() {
		CommentServiceApplication app = new CommentServiceApplication();
		assertNotNull(app);
	}
}