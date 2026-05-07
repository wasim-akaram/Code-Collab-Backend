/*
 * Code reader note: Provides RestTemplate for outbound calls from comment-service.
 */
package com.codesync.comment.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configures a load-balanced RestTemplate for calling other microservices
 * by their Eureka service names (e.g., http://notification-service/...).
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
