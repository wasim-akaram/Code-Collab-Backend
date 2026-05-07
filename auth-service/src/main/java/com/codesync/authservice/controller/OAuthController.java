/*
 * Code reader note: Provides the simple OAuth success endpoint used after external login completes.
 */
package com.codesync.authservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class OAuthController {

    @GetMapping("/oauth-success")
    public String success() {
        // Simple callback response after successful OAuth login.
        return "OAuth Login Successful";
    }
}