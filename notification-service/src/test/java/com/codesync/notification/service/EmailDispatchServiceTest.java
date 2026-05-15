package com.codesync.notification.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailDispatchService emailDispatchService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailDispatchService, "mailFrom", "test@domain.com");
    }

    @Test
    void sendEmail_Success() {
        emailDispatchService.sendEmail("to@test.com", "Subject", "Body");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_NullSubjectBody() {
        emailDispatchService.sendEmail("to@test.com", null, null);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendEmail_Exception_Caught() {
        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(SimpleMailMessage.class));
        emailDispatchService.sendEmail("to@test.com", "Subj", "Body");
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
