/*
 * Code reader note: Bootstraps the version snapshot service.
 */
package com.codesync.version;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main entry point for the CodeSync Version Service.
 * This service manages snapshots (commits), file histories, diffing, and branching.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class VersionServiceApplication {

    public static void main(String[] args) {
        // Starts the application and registers with Eureka
        SpringApplication.run(VersionServiceApplication.class, args);
    }
}
