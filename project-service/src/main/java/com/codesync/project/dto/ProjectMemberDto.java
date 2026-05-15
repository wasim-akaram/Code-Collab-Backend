/*
 * Code reader note: Defines the API payload shape used by project-service controllers and clients.
 */
package com.codesync.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO representing a project member.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDto {

    private Long id;
    private Long projectId;
    private String userEmail;
    private String role;         // OWNER | EDITOR | VIEWER
    private LocalDateTime createdAt;
}
