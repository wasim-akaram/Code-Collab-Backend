/*
 * Code reader note: Defines the API payload shape used by project-service controllers and clients.
 */
package com.codesync.project.dto;

import com.codesync.common.enums.ProjectVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectDto {
    
    private Long id;
    private String name;
    private String description;
    private Long ownerId;
    private String ownerEmail;
    private String language;
    private ProjectVisibility visibility;
    private Boolean archived;
    private Long starCount;
    private Long forkCount;
    private Long parentProjectId;
    private String defaultBranch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
