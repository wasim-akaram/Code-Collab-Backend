/*
 * Code reader note: Configures auth-service security rules, public endpoints, OAuth login, JWT filtering, CORS, and password encoding.
 */
package com.codesync.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.codesync.authservice.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final RedisTemplate<String, Object> redisTemplate;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CORS handled by API Gateway — disable here to prevent duplicate headers.
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Fully public endpoints — no token required.
                .requestMatchers(
                    "/auth/register",
                    "/auth/email-exists",
                    "/auth/verify-and-register",
                    "/auth/login",
                    "/auth/send-otp",
                    "/auth/verify-otp",
                    "/auth/refresh",
                    "/auth/forgot-password",
                    "/auth/reset-password",
                    "/auth/logout",
                    "/auth/search",
                    "/auth/users/**",
                    "/auth/health",
                    "/oauth2/**",
                    "/login/oauth2/**",
                    "/v3/api-docs/**",
                    "/auth/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/auth/swagger-ui/**",
                    "/swagger-ui.html",
                    "/auth/swagger-ui.html"
                ).permitAll()
                // Protected endpoints — JWT required.
                .requestMatchers(
                    "/auth/profile",
                    "/auth/password",
                    "/auth/deactivate"
                ).authenticated()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oAuth2LoginSuccessHandler)
            )
            // Pass RedisTemplate to JwtFilter for blacklist checking
            .addFilterBefore(new JwtFilter(jwtUtil, redisTemplate),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
