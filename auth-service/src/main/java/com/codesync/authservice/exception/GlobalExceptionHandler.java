/*
 * Code reader note: Converts auth-service runtime errors into HTTP responses.
 * Annotations used: @RestControllerAdvice applies the handler globally, and
 * @ExceptionHandler maps RuntimeException to a 400 response.
 */
package com.codesync.authservice.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handle(RuntimeException ex) {
        // Convert runtime exceptions into HTTP 400 with message body.
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}