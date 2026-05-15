/*
 * Code reader note: Bootstraps the file management service.
 */
package com.codesync.file;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Main entry point for the CodeSync File Service.
 * This service handles all file operations (CRUD, renaming, content editing).
 */
@SpringBootApplication
@EnableDiscoveryClient
public class FileServiceApplication {

    public static void main(String[] args) {
        // Starts the Spring Boot application and registers with Eureka
        SpringApplication.run(FileServiceApplication.class, args);
    }
}
