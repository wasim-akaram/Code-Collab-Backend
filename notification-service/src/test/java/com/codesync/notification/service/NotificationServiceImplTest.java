package com.codesync.notification.service;

import com.codesync.notification.dto.NotificationDto;
import com.codesync.notification.entity.Notification;
import com.codesync.notification.exception.ResourceNotFoundException;
import com.codesync.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Tests for NotificationServiceImpl.
 * NOTE: This service uses email-based identity (userEmail String), not Long userId.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;
    private NotificationDto dto;
    private static final String USER_EMAIL = "user@example.com";
    private static final String OTHER_EMAIL = "other@example.com";

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .notificationId(1L)
                .userEmail(USER_EMAIL)
                .actorEmail("actor@example.com")
                .title("Test Title")
                .type("COMMENT_MENTION")
                .message("You were mentioned")
                .referenceId(42L)
                .referenceType("COMMENT")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        dto = NotificationDto.builder()
                .userEmail(USER_EMAIL)
                .actorEmail("actor@example.com")
                .title("Test Title")
                .type("COMMENT_MENTION")
                .message("You were mentioned")
                .referenceId(42L)
                .referenceType("COMMENT")
                .build();
    }

    // ─── sendNotification ───────────────────────────────────────────────────────

    @Test
    void sendNotification_shouldPersistAndReturnDto() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationDto result = notificationService.sendNotification(dto);

        assertNotNull(result);
        assertEquals(1L, result.getNotificationId());
        assertEquals(USER_EMAIL, result.getUserEmail());
        assertEquals("COMMENT_MENTION", result.getType());
        assertEquals("You were mentioned", result.getMessage());
        assertFalse(result.getIsRead());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sendNotification_shouldMapAllFields() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationDto result = notificationService.sendNotification(dto);

        assertEquals("actor@example.com", result.getActorEmail());
        assertEquals("Test Title", result.getTitle());
        assertEquals(42L, result.getReferenceId());
        assertEquals("COMMENT", result.getReferenceType());
    }

    // ─── sendBulk ───────────────────────────────────────────────────────────────

    @Test
    void sendBulk_shouldSaveOneNotificationPerRecipient() {
        List<String> recipients = List.of("a@test.com", "b@test.com", "c@test.com");

        notificationService.sendBulk(recipients, "Title", "Message", "BROADCAST");

        verify(notificationRepository).saveAll(argThat((List<Notification> batch) ->
                batch.size() == 3 &&
                batch.stream().allMatch(n -> "BROADCAST".equals(n.getType()) && "Title".equals(n.getTitle()))
        ));
    }

    @Test
    void sendBulk_shouldDeduplicateRecipients() {
        List<String> recipients = List.of("a@test.com", "a@test.com", "b@test.com");

        notificationService.sendBulk(recipients, "Title", "Msg", "INFO");

        verify(notificationRepository).saveAll(argThat((List<Notification> batch) ->
                batch.size() == 2
        ));
    }

    @Test
    void sendBulk_shouldDefaultTypeToBroadcastWhenNull() {
        List<String> recipients = List.of("a@test.com");

        notificationService.sendBulk(recipients, "Title", "Msg", null);

        verify(notificationRepository).saveAll(argThat((List<Notification> batch) ->
                "BROADCAST".equals(batch.get(0).getType())
        ));
    }

    @Test
    void sendBulk_shouldDoNothingWhenRecipientsEmpty() {
        notificationService.sendBulk(List.of(), "Title", "Msg", "BROADCAST");
        verifyNoInteractions(notificationRepository);
    }

    @Test
    void sendBulk_shouldDoNothingWhenRecipientsNull() {
        notificationService.sendBulk(null, "Title", "Msg", "BROADCAST");
        verifyNoInteractions(notificationRepository);
    }

    // ─── getUserNotifications ───────────────────────────────────────────────────

    @Test
    void getUserNotifications_shouldMapResults() {
        when(notificationRepository.findByUserEmailOrderByCreatedAtDesc(USER_EMAIL))
                .thenReturn(List.of(notification));

        List<NotificationDto> results = notificationService.getUserNotifications(USER_EMAIL);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getNotificationId());
        assertEquals(USER_EMAIL, results.get(0).getUserEmail());
    }

    @Test
    void getUserNotifications_shouldReturnEmptyListWhenNone() {
        when(notificationRepository.findByUserEmailOrderByCreatedAtDesc(USER_EMAIL))
                .thenReturn(List.of());

        List<NotificationDto> results = notificationService.getUserNotifications(USER_EMAIL);
        assertTrue(results.isEmpty());
    }

    // ─── getUnreadNotifications ─────────────────────────────────────────────────

    @Test
    void getUnreadNotifications_shouldReturnOnlyUnread() {
        when(notificationRepository.findByUserEmailAndIsReadFalseOrderByCreatedAtDesc(USER_EMAIL))
                .thenReturn(List.of(notification));

        List<NotificationDto> results = notificationService.getUnreadNotifications(USER_EMAIL);

        assertEquals(1, results.size());
        assertFalse(results.get(0).getIsRead());
    }

    // ─── getUnreadCount ─────────────────────────────────────────────────────────

    @Test
    void getUnreadCount_shouldReturnCorrectCount() {
        when(notificationRepository.countByUserEmailAndIsReadFalse(USER_EMAIL)).thenReturn(5L);

        assertEquals(5L, notificationService.getUnreadCount(USER_EMAIL));
    }

    @Test
    void getUnreadCount_shouldReturnZeroWhenAllRead() {
        when(notificationRepository.countByUserEmailAndIsReadFalse(USER_EMAIL)).thenReturn(0L);

        assertEquals(0L, notificationService.getUnreadCount(USER_EMAIL));
    }

    // ─── markAsRead ─────────────────────────────────────────────────────────────

    @Test
    void markAsRead_shouldSetIsReadTrue() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationDto result = notificationService.markAsRead(1L, USER_EMAIL);

        assertTrue(result.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_shouldRejectDifferentUser() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(SecurityException.class,
                () -> notificationService.markAsRead(1L, OTHER_EMAIL));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_shouldThrowWhenNotificationMissing() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.markAsRead(1L, USER_EMAIL));
    }

    // ─── markAllAsRead ──────────────────────────────────────────────────────────

    @Test
    void markAllAsRead_shouldDelegateToRepository() {
        notificationService.markAllAsRead(USER_EMAIL);
        verify(notificationRepository).markAllAsReadForUser(USER_EMAIL);
    }

    // ─── deleteNotification ─────────────────────────────────────────────────────

    @Test
    void deleteNotification_shouldRemoveForOwner() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertDoesNotThrow(() -> notificationService.deleteNotification(1L, USER_EMAIL));
        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_shouldRejectDifferentUser() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(SecurityException.class,
                () -> notificationService.deleteNotification(1L, OTHER_EMAIL));
        verify(notificationRepository, never()).delete(any());
    }

    @Test
    void deleteNotification_shouldThrowWhenNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> notificationService.deleteNotification(99L, USER_EMAIL));
    }

    // ─── deleteAllNotifications ─────────────────────────────────────────────────

    @Test
    void deleteAllNotifications_shouldDelegateToRepository() {
        notificationService.deleteAllNotifications(USER_EMAIL);
        verify(notificationRepository).deleteByUserEmail(USER_EMAIL);
    }
}