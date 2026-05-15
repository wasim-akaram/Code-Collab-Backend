/*
 * Code reader note: Bootstraps the code comment service.
 */
package com.codesync.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the CodeSync Comment Service.
 * Handles inline code comments and code reviews.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class CommentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApplication.class, args);
    }
}
