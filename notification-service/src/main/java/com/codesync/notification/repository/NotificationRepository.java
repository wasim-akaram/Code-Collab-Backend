/*
 * Code reader note: Provides database queries for notifications by recipient, read state, type, priority, and creation time.
 */
package com.codesync.notification.repository;

import com.codesync.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Notification entities.
 * Uses userEmail (String) instead of userId (Long).
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Retrieve all notifications for a user, ordered newest first
    List<Notification> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    // Retrieve only unread notifications for a user
    List<Notification> findByUserEmailAndIsReadFalseOrderByCreatedAtDesc(String userEmail);

    // Count unread notifications for a user
    long countByUserEmailAndIsReadFalse(String userEmail);

    // Find notifications by type (e.g., all COMMENT_MENTION alerts)
    List<Notification> findByUserEmailAndType(String userEmail, String type);

    // Mark all unread notifications as read for a specific user
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userEmail = :userEmail AND n.isRead = false")
    void markAllAsReadForUser(String userEmail);

    // Delete all notifications for a user
    void deleteByUserEmail(String userEmail);
}
