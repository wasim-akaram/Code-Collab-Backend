package com.codesync.authservice.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handle_shouldReturnBadRequestWithMessage() {
        RuntimeException ex = new RuntimeException("bad input");

        ResponseEntity<String> response = handler.handle(ex);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("bad input", response.getBody());
    }
}
