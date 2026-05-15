package com.codesync.notification.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codesync.notification.dto.NotificationDto;
import com.codesync.notification.entity.Notification;
import com.codesync.notification.exception.ResourceNotFoundException;
import com.codesync.notification.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private EmailDispatchService emailDispatchService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private static final String USER = "user@test.com";

    @Test
    void sendNotification_Success_WithEmail() {
        NotificationDto dto = NotificationDto.builder()
                .userEmail(USER)
                .type("PROJECT_FORKED")
                .message("Test")
                .build();

        Notification saved = Notification.builder().notificationId(1L).type("PROJECT_FORKED").build();
        when(notificationRepository.save(any())).thenReturn(saved);

        NotificationDto result = notificationService.sendNotification(dto);

        assertNotNull(result);
        verify(emailDispatchService).sendEmail(eq(USER), any(), eq("Test"));
    }

    @Test
    void sendNotification_Success_NoEmail() {
        NotificationDto dto = NotificationDto.builder()
                .userEmail(USER)
                .type("INFO")
                .build();

        Notification saved = Notification.builder().notificationId(1L).type("INFO").build();
        when(notificationRepository.save(any())).thenReturn(saved);

        NotificationDto result = notificationService.sendNotification(dto);

        assertNotNull(result);
        verify(emailDispatchService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void sendBulk_Success() {
        notificationService.sendBulk(List.of(USER, USER, "other@test.com"), "Title", "Msg", "INFO");
        // Should only save 2 distinct notifications
        verify(notificationRepository).saveAll(argThat(list -> {
            List<Notification> l = (List<Notification>) list;
            return l.size() == 2;
        }));
    }

    @Test
    void sendBulk_NullList() {
        notificationService.sendBulk(null, "Title", "Msg", "INFO");
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    void getUserNotifications_Success() {
        Notification n = Notification.builder().notificationId(1L).build();
        when(notificationRepository.findByUserEmailOrderByCreatedAtDesc(USER)).thenReturn(List.of(n));

        List<NotificationDto> res = notificationService.getUserNotifications(USER);
        assertEquals(1, res.size());
    }

    @Test
    void getUnreadNotifications_Success() {
        Notification n = Notification.builder().notificationId(1L).build();
        when(notificationRepository.findByUserEmailAndIsReadFalseOrderByCreatedAtDesc(USER)).thenReturn(List.of(n));

        List<NotificationDto> res = notificationService.getUnreadNotifications(USER);
        assertEquals(1, res.size());
    }

    @Test
    void getUnreadCount_Success() {
        when(notificationRepository.countByUserEmailAndIsReadFalse(USER)).thenReturn(5L);
        assertEquals(5L, notificationService.getUnreadCount(USER));
    }

    @Test
    void markAsRead_Success() {
        Notification n = Notification.builder().notificationId(1L).userEmail(USER).isRead(false).build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any())).thenReturn(n);

        NotificationDto res = notificationService.markAsRead(1L, USER);
        assertTrue(res.getIsRead());
    }

    @Test
    void markAsRead_NotFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(1L, USER));
    }

    @Test
    void markAsRead_WrongUser() {
        Notification n = Notification.builder().notificationId(1L).userEmail("other@test.com").build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        assertThrows(SecurityException.class, () -> notificationService.markAsRead(1L, USER));
    }

    @Test
    void markAllAsRead_Success() {
        notificationService.markAllAsRead(USER);
        verify(notificationRepository).markAllAsReadForUser(USER);
    }

    @Test
    void deleteNotification_Success() {
        Notification n = Notification.builder().notificationId(1L).userEmail(USER).build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));

        notificationService.deleteNotification(1L, USER);
        verify(notificationRepository).delete(n);
    }

    @Test
    void deleteNotification_NotFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> notificationService.deleteNotification(1L, USER));
    }

    @Test
    void deleteNotification_WrongUser() {
        Notification n = Notification.builder().notificationId(1L).userEmail("other@test.com").build();
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(n));
        assertThrows(SecurityException.class, () -> notificationService.deleteNotification(1L, USER));
    }

    @Test
    void deleteAllNotifications_Success() {
        notificationService.deleteAllNotifications(USER);
        verify(notificationRepository).deleteByUserEmail(USER);
    }

    @Test
    void sendEmailAlert_Success() {
        notificationService.sendEmailAlert(USER, "Subj", "Body");
        verify(emailDispatchService).sendEmail(USER, "Subj", "Body");
    }
}
