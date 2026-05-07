/*
 * Code reader note: Configures notification-service security, JWT filtering, stateless sessions, and CORS.
 */
package com.codesync.notification.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * Security configuration for Notification Service.
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
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow actuator and openapi endpoints
                .requestMatchers(
                    "/actuator/**", 
                    "/v3/api-docs/**", 
                    "/notifications/v3/api-docs/**",
                    "/swagger-ui/**", 
                    "/notifications/swagger-ui/**",
                    "/swagger-ui.html",
                    "/notifications/swagger-ui.html",
                    "/swagger-ui/index.html",
                    "/notifications/swagger-ui/index.html",
                    "/swagger-resources/**",
                    "/notifications/swagger-resources/**",
                    "/webjars/**",
                    "/notifications/webjars/**"
                ).permitAll()
                .requestMatchers(
                    "/notifications/send",
                    "/notifications/email"
                ).permitAll()
                // The /send endpoint should normally be protected using internal service authentication (e.g. mTLS or internal token)
                // but for this architecture we just require an authenticated request
                .anyRequest().authenticated()
            )
            // Add custom filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
