/*
 * Code reader note: Configures the STOMP broker, websocket endpoint, and topic/application destinations.
 */
package com.codesync.collab.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration for WebSocket with STOMP protocol.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for sending messages to clients
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for messages sent from clients to the server
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the STOMP endpoint for clients to connect
        registry.addEndpoint("/ws-collab")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Fallback for browsers without WebSocket support
    }
}
