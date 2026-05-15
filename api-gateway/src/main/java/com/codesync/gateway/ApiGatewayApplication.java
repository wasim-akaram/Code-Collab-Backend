/*
 * Code reader note: Starts the Spring Cloud Gateway service, the single HTTP entry point that routes frontend requests to backend services.
 */
package com.codesync.gateway;

import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * API Gateway - Routes requests to microservices
 */
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
