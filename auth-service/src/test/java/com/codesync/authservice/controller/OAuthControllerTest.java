package com.codesync.authservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OAuthControllerTest {

    private final OAuthController controller = new OAuthController();

    @Test
    void success_shouldReturnExpectedMessage() {
        assertEquals("OAuth Login Successful", controller.success());
    }
}
