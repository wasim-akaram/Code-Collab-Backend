/*
 * Code reader note: Bootstraps the user authentication service.
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
