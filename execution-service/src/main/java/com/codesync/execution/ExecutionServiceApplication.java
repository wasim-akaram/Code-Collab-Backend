/*
 * Code reader note: Bootstraps the code execution service and provides shared HTTP client support.
 */
package com.codesync.execution;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

/**
 * Main entry point for the CodeSync Execution Service.
 * This service handles code compilation and execution in isolated environments.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
public class ExecutionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExecutionServiceApplication.class, args);
    }
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
