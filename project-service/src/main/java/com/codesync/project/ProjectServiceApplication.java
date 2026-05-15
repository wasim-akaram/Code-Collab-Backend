/*
 * Code reader note: Bootstraps the project management service and scans shared common-lib classes.
 */
package com.codesync.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Project Service - Project management (CRUD, visibility, forking, starring)
 */
@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.codesync.project", "com.codesync.common"})
public class ProjectServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjectServiceApplication.class, args);
    }
}
