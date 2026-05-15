/*
 * Code reader note: Defines notification operations implemented by the service layer.
 */
package com.codesync.notification.service;

import com.codesync.notification.dto.NotificationDto;

import java.util.List;

/**
 * Interface defining notification operations.
 * Uses email-based identity (userEmail) to match the API Gateway X-User header.
 * Matches spec §4.8 method set.
 */
public interface NotificationService {

    // Create and persist a new notification for a user
    NotificationDto sendNotification(NotificationDto dto);

    /**
     * Send the same notification to multiple recipients (admin broadcast).
     * Creates one Notification row per email in recipientEmails.
     */
    void sendBulk(List<String> recipientEmails, String title, String message, String type);

    // Retrieve all notifications for a user (newest first)
    List<NotificationDto> getUserNotifications(String userEmail);

    // Retrieve only unread notifications for a user
    List<NotificationDto> getUnreadNotifications(String userEmail);

    // Get count of unread notifications
    long getUnreadCount(String userEmail);

    // Mark a specific notification as read (validates ownership)
    NotificationDto markAsRead(Long notificationId, String userEmail);

    // Mark all notifications as read for a user
    void markAllAsRead(String userEmail);

    // Delete a single notification (validates ownership)
    void deleteNotification(Long notificationId, String userEmail);

    // Delete all notifications for a user
    void deleteAllNotifications(String userEmail);

    // Send an async email alert (fire-and-forget)
    void sendEmailAlert(String toEmail, String subject, String body);
}
