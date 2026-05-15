/*
 * Code reader note: Provides database queries for users participating in collaboration sessions.
 */
package com.codesync.collab.repository;

import com.codesync.collab.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing participants in collaboration sessions.
 * Uses userEmail (String) instead of userId (Long) — Option A.
 */
@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    // Find all active (not yet left) participants in a session
    List<Participant> findBySessionIdAndLeftAtIsNull(String sessionId);

    // Find all participants (including those who left) in a session
    List<Participant> findBySessionId(String sessionId);

    // Find a specific active participant by email in a session
    Optional<Participant> findBySessionIdAndUserEmailAndLeftAtIsNull(String sessionId, String userEmail);

    // Count active participants in a session
    long countBySessionIdAndLeftAtIsNull(String sessionId);

    // Find sessions a user is currently active in
    List<Participant> findByUserEmailAndLeftAtIsNull(String userEmail);
}
