package com.codesync.admin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Security configuration for the Spring Boot Admin Dashboard.
 * This ensures that the infrastructure dashboard is protected by a login page,
 * while still allowing other microservices to register and communicate with it.
 */
@Configuration
public class SecurityConfig {

    private final String adminContextPath;

    public SecurityConfig(AdminServerProperties adminServerProperties) {
        // Retrieve the context path (defaults to "/" unless specified in application.yml)
        this.adminContextPath = adminServerProperties.getContextPath();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Success handler to redirect users back to the dashboard after a successful login
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminContextPath + "/");

        http.authorizeHttpRequests(authorize -> authorize
                // Allow public access to static assets (CSS, JS, images) and the login page
                .requestMatchers(this.adminContextPath + "/assets/**").permitAll()
                .requestMatchers(this.adminContextPath + "/login").permitAll()
                // Require authentication for all other requests (the actual dashboard data)
                .anyRequest().authenticated()
            )
            .formLogin(formLogin -> formLogin
                // Use the built-in Spring Boot Admin login page
                .loginPage(this.adminContextPath + "/login")
                .successHandler(successHandler)
            )
            .logout(logout -> logout
                // Configure the logout URL
                .logoutUrl(this.adminContextPath + "/logout")
            )
            // Enable HTTP Basic Auth for microservices registering directly (if they don't use Eureka)
            .httpBasic(httpBasic -> {})
            .csrf(csrf -> csrf
                // Use cookies for CSRF tokens so the frontend JS can read them
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                // Ignore CSRF protection for specific endpoints so microservices can send data (instances, actuator) without needing a token
                .ignoringRequestMatchers(
                    this.adminContextPath + "/instances",
                    this.adminContextPath + "/actuator/**"
                )
            );

        return http.build();
    }
}
