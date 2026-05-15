package com.codesync.comment.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationClient notificationClient;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationClient, "restTemplate", restTemplate);
    }

    @Test
    void testSendMentionNotification_Success() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Object.class))).thenReturn("OK");

        assertDoesNotThrow(() -> notificationClient.sendMentionNotification("mentioned@test.com", "author@test.com", 1L, 2L));

        verify(restTemplate).postForObject(anyString(), any(HttpEntity.class), eq(Object.class));
    }

    @Test
    void testSendMentionNotification_Exception() {
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Object.class))).thenThrow(new RuntimeException("Error"));

        assertDoesNotThrow(() -> notificationClient.sendMentionNotification("mentioned@test.com", "author@test.com", 1L, 2L));
    }
}
