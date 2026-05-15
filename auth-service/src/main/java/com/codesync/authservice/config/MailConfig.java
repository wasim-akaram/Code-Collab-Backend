/*
 * Code reader note: Defines the async mail executor so email sending does not block
 * request handling.
 * Annotations used: @Configuration registers the config class, @EnableAsync enables
 * @Async methods, and @Bean publishes the named mailExecutor thread pool.
 */
package com.codesync.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Mail & Async configuration.
 *
 * JavaMailSender is auto-configured by Spring Boot from application.yaml:
 *   spring.mail.host / port / username / password / properties
 * No manual JavaMailSender bean is needed here (avoids credential override).
 *
 * @EnableAsync activates @Async on EmailService.sendOtp() so that SMTP calls
 * run on a dedicated thread pool and never block the HTTP request thread.
 */
@Configuration
@EnableAsync
public class MailConfig {

    /**
     * Named executor used by @Async("mailExecutor").
     * Bounded pool (core=2, max=5) prevents thread starvation under load.
     */
    @Bean(name = "mailExecutor")
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("mail-async-");
        executor.setWaitForTasksToCompleteOnShutdown(false); // don't delay shutdown
        executor.initialize();
        return executor;
    }
}