/*
 * Code reader note: Configures execution-service security, JWT filtering, stateless sessions, and CORS.
 */
package com.codesync.execution.config;

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
 * Security configuration for Execution Service.
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
            .cors(cors -> cors.disable())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/**", 
                    "/v3/api-docs/**", 
                    "/executions/v3/api-docs/**",
                    "/swagger-ui/**", 
                    "/executions/swagger-ui/**",
                    "/swagger-ui.html",
                    "/executions/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/executions/swagger-ui/index.html",
                    "/swagger-resources/**",
                    "/executions/swagger-resources/**",
                    "/webjars/**",
                    "/executions/webjars/**",
                    "/executions/languages",
                    "/executions/languages/*/version"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
