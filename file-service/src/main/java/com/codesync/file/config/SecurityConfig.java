/*
 * Code reader note: Configures file-service security, public allowances, JWT filtering, stateless sessions, and CORS.
 */
package com.codesync.file.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for File Service.
 * CORS is handled by the API Gateway — disabled here to prevent duplicate headers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS handled by API Gateway — disable here to prevent duplicate headers.
            .cors(cors -> cors.disable())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow actuator and openapi endpoints
                .requestMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/files/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/files/swagger-ui/**",
                    "/swagger-ui.html",
                    "/files/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/files/swagger-ui/index.html",
                    "/swagger-resources/**",
                    "/files/swagger-resources/**",
                    "/webjars/**",
                    "/files/webjars/**"
                ).permitAll()
                // All other endpoints require authentication (X-User header from gateway)
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
