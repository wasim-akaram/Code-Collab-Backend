/*
 * Code reader note: Configures project-service authorization, JWT filtering, stateless sessions, CORS, and RestTemplate support.
 */
package com.codesync.project.config;

import com.codesync.project.util.JwtUtil;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtUtil jwtUtil) throws Exception {
        http
            // CORS is handled by the API Gateway — disable Spring Security's CORS processing
            // to prevent duplicate 'Access-Control-Allow-Origin' headers.
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/projects/public",
                    "/projects/search",
                    "/projects/trending",
                    "/projects/by-language",
                    "/v3/api-docs/**",
                    "/projects/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/projects/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-resources/**",
                    "/webjars/**",
                    "/actuator/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtFilter(),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** Load-balanced RestTemplate for inter-service calls (e.g., notification-service). */
    @Bean
    @org.springframework.cloud.client.loadbalancer.LoadBalanced
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}
