package com.codesync.authservice.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendOtp_shouldSendExpectedMessage() {
        emailService.sendOtp("john@example.com", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertArrayEquals(new String[] {"john@example.com"}, sent.getTo());
        // Use contains to avoid encoding issues with special Unicode characters in the subject
        assertTrue(sent.getSubject().contains("CodeSync") && sent.getSubject().contains("OTP Verification"),
                "Subject should contain 'CodeSync' and 'OTP Verification' but was: " + sent.getSubject());
        assertTrue(sent.getText().contains("123456"), "OTP should appear in email body");
    }
}
