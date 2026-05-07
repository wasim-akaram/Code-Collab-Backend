/*
 * Code reader note: Transfers collaboration session data through REST APIs.
 */
package com.codesync.collab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for sending collaboration session data to clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollabSessionDto {
    private String sessionId;
    private Long projectId;
    private Long fileId;
    private String ownerEmail;
    private String status;
    private String language;
    private Integer maxParticipants;
    private Boolean isPasswordProtected;
    private String sessionPassword;       // Only used on creation request; never returned
    private Integer participantCount;     // Optional: included on GET responses
    private LocalDateTime createdAt;
    private LocalDateTime endedAt;
}
