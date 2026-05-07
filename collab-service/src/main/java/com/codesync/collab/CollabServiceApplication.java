/*
 * Code reader note: Bootstraps the live collaboration service.
 */
package com.codesync.collab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the CodeSync Collaboration Service.
 * This service manages real-time live coding sessions via WebSocket (STOMP).
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@EnableAsync
public class CollabServiceApplication {

    public static void main(String[] args) {
        // Bootstraps the application and registers with Eureka
        SpringApplication.run(CollabServiceApplication.class, args);
    }
}
