/*
 * Code reader note: Bootstraps the notification service.
 */
package com.codesync.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the CodeSync Notification Service.
 * Manages all in-app alerts, read status, and email dispatches.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
