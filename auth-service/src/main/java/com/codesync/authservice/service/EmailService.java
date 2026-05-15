/*
 * Code reader note: Sends OTP email messages using Spring mail configuration.
 * Annotations used: @Service registers the mail service, @RequiredArgsConstructor
 * injects the mail sender, @Slf4j adds the logger, @Value reads the sender address,
 * @PostConstruct runs startup diagnostics, and @Async("mailExecutor") sends mail off
 * the request thread.
 */
package com.codesync.authservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @PostConstruct
    public void debugMailConfig() {
        log.info("===== MAIL CONFIG DEBUG =====");
        log.info("Configured sender address: {}", mailUsername);
        log.info("=============================");
    }

    /**
     * Sends an OTP email asynchronously on the dedicated mail thread pool.
     * Uses the named executor "mailExecutor" defined in MailConfig.
     * Never blocks the HTTP request thread — registration returns immediately.
     */
    @Async("mailExecutor")
    public void sendOtp(String toEmail, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setFrom(mailUsername);
        message.setSubject("CodeSync — Your OTP Verification Code");
        message.setText(
            "Hi,\n\n" +
            "Your CodeSync one-time verification code is:\n\n" +
            "  " + otp + "\n\n" +
            "This code expires in 5 minutes.\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "— The CodeSync Team"
        );

        try {
            mailSender.send(message);
            log.info("OTP email sent successfully to {}", toEmail);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("Failed to send OTP email to {}. Root cause: {} — {}",
                      toEmail, cause.getClass().getSimpleName(), cause.getMessage());
            // Fallback: print OTP to log so dev/testing can still proceed
            log.warn(">>>>> FALLBACK OTP for {} is: {} <<<<<", toEmail, otp);
            log.warn("To fix: ensure Gmail 2-Step is ON and App Password at " +
                     "https://myaccount.google.com/apppasswords matches application.yaml");
        }
    }
}