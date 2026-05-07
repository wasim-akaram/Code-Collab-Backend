package com.codesync.notification.controller;

import com.codesync.notification.dto.NotificationDto;
import com.codesync.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Fixed NotificationControllerTest — all methods use email (String) not userId (Long),
 * matching the current real service implementation.
 */
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private NotificationService notificationService;

    private static final String USER_EMAIL = "user@example.com";

    private NotificationDto notificationDto;

    @BeforeEach
    void setUp() {
        // NotificationDto now uses userEmail (String), not userId (Long)
        notificationDto = NotificationDto.builder()
                .notificationId(1L)
                .userEmail(USER_EMAIL)
                .actorEmail("actor@example.com")
                .type("COMMENT_MENTION")
                .message("You were mentioned")
                .referenceId(42L)
                .referenceType("COMMENT")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void sendNotification_shouldReturnCreatedNotification() throws Exception {
        when(notificationService.sendNotification(any(NotificationDto.class))).thenReturn(notificationDto);

        mockMvc.perform(post("/notifications/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(notificationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationId").value(1L))
                .andExpect(jsonPath("$.type").value("COMMENT_MENTION"));

        verify(notificationService).sendNotification(any(NotificationDto.class));
    }

    @Test
    void getUserNotifications_shouldReturnList() throws Exception {
        // Controller reads X-User header as email string
        when(notificationService.getUserNotifications(USER_EMAIL)).thenReturn(List.of(notificationDto));

        mockMvc.perform(get("/notifications").header("X-User", USER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value(1L));

        verify(notificationService).getUserNotifications(USER_EMAIL);
    }

    @Test
    void getUnreadNotifications_shouldReturnList() throws Exception {
        when(notificationService.getUnreadNotifications(USER_EMAIL)).thenReturn(List.of(notificationDto));

        mockMvc.perform(get("/notifications/unread").header("X-User", USER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(false));

        verify(notificationService).getUnreadNotifications(USER_EMAIL);
    }

    @Test
    void getUnreadCount_shouldReturnCount() throws Exception {
        when(notificationService.getUnreadCount(USER_EMAIL)).thenReturn(3L);

        mockMvc.perform(get("/notifications/unread/count").header("X-User", USER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3L));

        verify(notificationService).getUnreadCount(USER_EMAIL);
    }

    @Test
    void markAsRead_shouldReturnUpdatedNotification() throws Exception {
        NotificationDto read = NotificationDto.builder()
                .notificationId(1L)
                .userEmail(USER_EMAIL)
                .isRead(true)
                .build();
        when(notificationService.markAsRead(1L, USER_EMAIL)).thenReturn(read);

        mockMvc.perform(put("/notifications/1/read").header("X-User", USER_EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));

        verify(notificationService).markAsRead(1L, USER_EMAIL);
    }

    @Test
    void markAllAsRead_shouldReturnNoContent() throws Exception {
        doNothing().when(notificationService).markAllAsRead(USER_EMAIL);

        mockMvc.perform(put("/notifications/read/all").header("X-User", USER_EMAIL))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(USER_EMAIL);
    }

    @Test
    void sendEmailAlert_shouldReturnOk() throws Exception {
        doNothing().when(notificationService).sendEmailAlert(any(), any(), any());

        mockMvc.perform(post("/notifications/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                            "toEmail", "dev@example.com",
                            "subject", "Hello",
                            "body", "World"
                        ))))
                .andExpect(status().isAccepted());

        verify(notificationService).sendEmailAlert("dev@example.com", "Hello", "World");
    }
}