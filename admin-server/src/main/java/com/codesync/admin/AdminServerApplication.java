package com.codesync.admin;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * The main entry point for the Spring Boot Admin Server.
 * This microservice acts as an infrastructure dashboard to monitor the health, 
 * metrics, and logs of all other microservices in the CodeSync architecture.
 */
@SpringBootApplication
@EnableAdminServer // Turns this Spring Boot application into a Spring Boot Admin Server
@EnableDiscoveryClient // Allows this server to act as a Eureka Client to auto-discover other microservices
public class AdminServerApplication {

    public static void main(String[] args) {
        // Bootstraps and launches the Admin Server
        SpringApplication.run(AdminServerApplication.class, args);
    }
}
