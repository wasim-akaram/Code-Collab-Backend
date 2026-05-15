/*
 * Code reader note: Implements notification creation, bulk sends, unread tracking,
 * read/delete actions, and email alerts.
 * Annotations used: @Slf4j provides logging, @Service registers the service bean,
 * @RequiredArgsConstructor injects dependencies, and @Transactional keeps write
 * operations consistent.
 */
package com.codesync.notification.service;

import com.codesync.notification.dto.NotificationDto;
import com.codesync.notification.entity.Notification;
import com.codesync.notification.exception.ResourceNotFoundException;
import com.codesync.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of NotificationService.
 * Uses email-based identity (userEmail) — consistent with all other services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailDispatchService emailDispatchService;

    @Override
    @Transactional
    public NotificationDto sendNotification(NotificationDto dto) {
        // actorEmail is optional and represents the user who caused the event,
        // while userEmail is always the notification recipient.
        Notification notification = Notification.builder()
                .userEmail(dto.getUserEmail())
                .actorEmail(dto.getActorEmail())
                .title(dto.getTitle())
                .type(dto.getType())
                .message(dto.getMessage())
                .referenceId(dto.getReferenceId())
                .referenceType(dto.getReferenceType())
                .build();

        NotificationDto saved = mapToDto(notificationRepository.save(notification));
        log.info("Notification sent to {}: [{}] {}", dto.getUserEmail(), dto.getType(), dto.getMessage());

        if (shouldEmail(dto.getType())) {
            emailDispatchService.sendEmail(dto.getUserEmail(), dto.getTitle(), dto.getMessage());
        }

        return saved;
    }

    /**
     * Admin broadcast — creates one notification row per recipient.
     * Runs in a transaction so all-or-nothing persists.
     */
    @Override
    @Transactional
    public void sendBulk(List<String> recipientEmails, String title, String message, String type) {
        if (recipientEmails == null || recipientEmails.isEmpty()) return;

        List<Notification> batch = recipientEmails.stream()
                // Prevent duplicate notifications when the caller accidentally
                // provides the same recipient more than once.
                .distinct()
                .map(email -> Notification.builder()
                        .userEmail(email)
                        .title(title)
                        .type(type != null ? type : "BROADCAST")
                        .message(message)
                        .referenceType("BROADCAST")
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(batch);
        log.info("Bulk notification sent to {} recipients: [{}] {}", batch.size(), type, title);
    }

    @Override
    public List<NotificationDto> getUserNotifications(String userEmail) {
        return notificationRepository.findByUserEmailOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<NotificationDto> getUnreadNotifications(String userEmail) {
        return notificationRepository.findByUserEmailAndIsReadFalseOrderByCreatedAtDesc(userEmail)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public long getUnreadCount(String userEmail) {
        return notificationRepository.countByUserEmailAndIsReadFalse(userEmail);
    }

    @Override
    @Transactional
    public NotificationDto markAsRead(Long notificationId, String userEmail) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUserEmail().equals(userEmail)) {
            throw new SecurityException("Cannot modify notifications belonging to another user");
        }

        // Marking as read is idempotent; repeating the request leaves the row in
        // the same state.
        notification.setIsRead(true);
        return mapToDto(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(String userEmail) {
        notificationRepository.markAllAsReadForUser(userEmail);
        log.info("Marked all notifications as read for {}", userEmail);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, String userEmail) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUserEmail().equals(userEmail)) {
            throw new SecurityException("Cannot delete notifications belonging to another user");
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void deleteAllNotifications(String userEmail) {
        notificationRepository.deleteByUserEmail(userEmail);
        log.info("Deleted all notifications for {}", userEmail);
    }

    @Override
    public void sendEmailAlert(String toEmail, String subject, String body) {
        emailDispatchService.sendEmail(toEmail, subject, body);
    }

    private boolean shouldEmail(String type) {
        if (type == null) return false;
        String normalized = type.trim().toUpperCase();
        return "PROJECT_FORKED".equals(normalized) || "PROJECT_MEMBER_ADDED".equals(normalized);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────────

    private NotificationDto mapToDto(Notification notification) {
        return NotificationDto.builder()
                .notificationId(notification.getNotificationId())
                .userEmail(notification.getUserEmail())
                .actorEmail(notification.getActorEmail())
                .title(notification.getTitle())
                .type(notification.getType())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
