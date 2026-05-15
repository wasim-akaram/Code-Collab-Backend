/*
 * Code reader note: Bootstraps the auth-service application.
 * Annotations used: @SpringBootApplication enables auto-configuration and component scanning,
 * and @EnableAsync turns on async execution for mail and other background tasks.
 */
package com.codesync.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AuthServiceApplication {

	public static void main(String[] args) {
		// Bootstraps Spring context and starts embedded server.
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
