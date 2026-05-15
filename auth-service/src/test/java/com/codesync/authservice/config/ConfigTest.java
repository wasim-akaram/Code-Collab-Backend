package com.codesync.authservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.codesync.authservice.util.JwtUtil;
import com.codesync.authservice.config.OAuth2LoginSuccessHandler;

class ConfigTest {

    @Test
    void securityFilterChain_shouldBuildConfiguredChain() throws Exception {
        OAuth2LoginSuccessHandler oauthHandler = mock(OAuth2LoginSuccessHandler.class);
        RedisTemplate<String, Object> redisMock = mock(RedisTemplate.class);
        SecurityConfig config = new SecurityConfig(new JwtUtil("5h14ouuPuDcEpQlSOrL7zxtiToeBrLhtWPtE1CIIfUM"), oauthHandler, redisMock);
        HttpSecurity http = mock(HttpSecurity.class);
        DefaultSecurityFilterChain chain = mock(DefaultSecurityFilterChain.class);

        when(http.cors(any())).thenReturn(http);
        when(http.csrf(any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.oauth2Login(any())).thenReturn(http);
        when(http.addFilterBefore(any(), any())).thenReturn(http);
        when(http.build()).thenReturn(chain);

        SecurityFilterChain result = config.securityFilterChain(http);

        assertSame(chain, result);
        verify(http).cors(any());
        verify(http).csrf(any());
        verify(http).authorizeHttpRequests(any());
        verify(http).oauth2Login(any());
        verify(http).addFilterBefore(any(), any());
        verify(http).build();
    }

    @Test
    void passwordEncoder_shouldBeBcrypt() {
        OAuth2LoginSuccessHandler oauthHandler = mock(OAuth2LoginSuccessHandler.class);
        RedisTemplate<String, Object> redisMock = mock(RedisTemplate.class);
        SecurityConfig config = new SecurityConfig(new JwtUtil("5h14ouuPuDcEpQlSOrL7zxtiToeBrLhtWPtE1CIIfUM"), oauthHandler, redisMock);

        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder);
        assertTrue(encoder.matches("secret", encoder.encode("secret")));
    }

    @Test
    void redisTemplate_shouldUseProvidedConnectionFactory() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory connectionFactory = org.mockito.Mockito.mock(RedisConnectionFactory.class);

        RedisTemplate<String, Object> template = config.redisTemplate(connectionFactory);

        assertSame(connectionFactory, template.getConnectionFactory());
    }

    @Test
    void mailConfig_shouldBeInstantiable() {
        // JavaMailSender is now auto-configured by Spring Boot from application.yaml.
        // MailConfig no longer defines a manual bean, so we just verify it can be instantiated.
        MailConfig config = new MailConfig();
        assertNotNull(config);
    }
}
