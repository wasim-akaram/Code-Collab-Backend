package com.codesync.notification.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.codesync.notification.dto.NotificationDto;
import com.codesync.notification.service.NotificationService;

/** Unit tests for {@link NotificationController}. */
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock private NotificationService notificationService;
    @InjectMocks private NotificationController controller;
    private static final String USER = "user@test.com";

    @Test @DisplayName("sendNotification returns 201")
    void sendNotification() {
        NotificationDto dto = new NotificationDto();
        when(notificationService.sendNotification(any())).thenReturn(dto);
        assertEquals(HttpStatus.CREATED, controller.sendNotification(dto).getStatusCode());
    }

    @Test @DisplayName("sendBulk returns 202")
    void sendBulk() {
        NotificationDto dto = new NotificationDto();
        dto.setRecipientEmails(List.of("a@t.com", "b@t.com"));
        dto.setTitle("Broadcast");
        dto.setMessage("Hello");
        dto.setType("BROADCAST");
        doNothing().when(notificationService).sendBulk(any(), any(), any(), any());
        assertEquals(HttpStatus.ACCEPTED, controller.sendBulk(dto).getStatusCode());
    }

    @Test @DisplayName("getUserNotifications returns list")
    void getUserNotifications() {
        when(notificationService.getUserNotifications(USER)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getUserNotifications(USER).getStatusCode());
    }

    @Test @DisplayName("getUnreadNotifications returns unread list")
    void getUnreadNotifications() {
        when(notificationService.getUnreadNotifications(USER)).thenReturn(List.of());
        assertEquals(HttpStatus.OK, controller.getUnreadNotifications(USER).getStatusCode());
    }

    @Test @DisplayName("getUnreadCount returns count map")
    void getUnreadCount() {
        when(notificationService.getUnreadCount(USER)).thenReturn(5L);
        assertEquals(5L, controller.getUnreadCount(USER).getBody().get("count"));
    }

    @Test @DisplayName("markAsRead returns updated notification")
    void markAsRead() {
        NotificationDto dto = new NotificationDto();
        when(notificationService.markAsRead(1L, USER)).thenReturn(dto);
        assertEquals(HttpStatus.OK, controller.markAsRead(1L, USER).getStatusCode());
    }

    @Test @DisplayName("markAllAsRead returns 204")
    void markAllAsRead() {
        doNothing().when(notificationService).markAllAsRead(USER);
        assertEquals(HttpStatus.NO_CONTENT, controller.markAllAsRead(USER).getStatusCode());
    }

    @Test @DisplayName("deleteNotification returns 204")
    void deleteNotification() {
        doNothing().when(notificationService).deleteNotification(1L, USER);
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteNotification(1L, USER).getStatusCode());
    }

    @Test @DisplayName("deleteAllNotifications returns 204")
    void deleteAllNotifications() {
        doNothing().when(notificationService).deleteAllNotifications(USER);
        assertEquals(HttpStatus.NO_CONTENT, controller.deleteAllNotifications(USER).getStatusCode());
    }

    @Test @DisplayName("sendEmailAlert returns 202")
    void sendEmailAlert() {
        doNothing().when(notificationService).sendEmailAlert(any(), any(), any());
        var payload = Map.of("toEmail", "u@t.com", "subject", "Test", "body", "Hello");
        assertEquals(HttpStatus.ACCEPTED, controller.sendEmailAlert(payload).getStatusCode());
    }
}