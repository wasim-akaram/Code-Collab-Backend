package com.codesync.authservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.codesync.authservice.entity.User;
import com.codesync.authservice.repository.UserRepository;
import com.codesync.authservice.service.OtpService;

@ExtendWith(MockitoExtension.class)
class OtpControllerTest {

    @Mock
    private OtpService otpService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OtpController otpController;

    @Test
    void sendOtp_shouldReturnBadRequestWhenEmailAlreadyRegistered() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(User.builder().build()));

        ResponseEntity<String> response = otpController.sendOtp("john@example.com");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Email already registered", response.getBody());
    }

    @Test
    void sendOtp_shouldReturnOkWhenEmailNotRegistered() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        ResponseEntity<String> response = otpController.sendOtp("john@example.com");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("OTP sent", response.getBody());
        verify(otpService).generateOtp("john@example.com");
    }

    @Test
    void verifyOtp_shouldReturnOkWhenOtpIsValid() {
        when(otpService.verifyOtp("john@example.com", "123456")).thenReturn(true);

        ResponseEntity<String> response = otpController.verifyOtp("john@example.com", "123456");

        assertEquals(200, response.getStatusCode().value());
        assertEquals("OTP verified", response.getBody());
    }

    @Test
    void verifyOtp_shouldReturnBadRequestWhenOtpIsInvalid() {
        when(otpService.verifyOtp("john@example.com", "000000")).thenReturn(false);

        ResponseEntity<String> response = otpController.verifyOtp("john@example.com", "000000");

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Invalid OTP", response.getBody());
    }
}
