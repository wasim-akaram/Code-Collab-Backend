/*
 * Code reader note: Provides a reusable gateway filter that logs request and response timing around proxied calls.
 */
package com.codesync.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * RequestLoggingFilter - Logs all gateway requests and responses
 * 
 * Features:
 * - Assigns unique request ID for tracing
 * - Logs request details (method, path, headers)
 * - Measures and logs response time
 * - Logs response status code
 * - Helps with debugging inter-service communication
 */
@Slf4j
@Component
public class RequestLoggingFilter extends AbstractGatewayFilterFactory<RequestLoggingFilter.Config> {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String START_TIME_ATTRIBUTE = "gateway.request.start.time";

    public RequestLoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Generate unique request ID for tracking
            String requestId = UUID.randomUUID().toString();
            
            // Log request details
            log.info("[{}] {} {} - Headers: Content-Type={}, Authorization={}", 
                requestId,
                request.getMethod(),
                request.getPath(),
                request.getHeaders().getFirst("Content-Type"),
                request.getHeaders().getFirst("Authorization") != null ? "***" : "None"
            );
            
            // Record start time
            exchange.getAttributes().put(START_TIME_ATTRIBUTE, System.currentTimeMillis());
            
            // Add request ID to downstream services
            ServerHttpRequest mutatedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();
            
            // Continue with mutated request
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    // Log response details
                    ServerHttpResponse response = exchange.getResponse();
                    Long startTime = exchange.getAttribute(START_TIME_ATTRIBUTE);
                    long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
                    
                    log.info("[{}] Response {} - Duration: {}ms", 
                        requestId,
                        response.getStatusCode(),
                        duration
                    );
                }));
        };
    }

    /**
     * Configuration class for request logging filter
     */
    public static class Config {
        // Add configuration properties if needed in future
    }
}
