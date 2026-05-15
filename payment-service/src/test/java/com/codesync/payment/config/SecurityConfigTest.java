package com.codesync.payment.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that {@link SecurityConfig} produces a valid SecurityFilterChain bean.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:sectest",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=test-jwt-secret-key-for-unit-testing-only-must-be-long",
    "razorpay.key-id=rzp_test_000000000000000",
    "razorpay.key-secret=test_secret_key_for_unit_tests_only",
    "auth-service.url=http://localhost:9090",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class SecurityConfigTest {

    @Autowired
    private SecurityFilterChain filterChain;

    @Test
    @DisplayName("SecurityFilterChain bean should be created")
    void filterChain_shouldExist() {
        assertNotNull(filterChain, "SecurityFilterChain should be wired");
    }
}
