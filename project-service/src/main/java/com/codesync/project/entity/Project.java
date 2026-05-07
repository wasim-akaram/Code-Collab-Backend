/*
 * Code reader note: Represents a code project with metadata, owner, visibility, language, and engagement counters.
 */
package com.codesync.project.entity;

import com.codesync.common.entity.BaseEntity;
import com.codesync.common.enums.ProjectVisibility;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Project entity representing a CodeSync project
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "projects", indexes = {
        @Index(name = "idx_owner_email", columnList = "ownerEmail"),
        @Index(name = "idx_visibility", columnList = "visibility"),
        @Index(name = "idx_archived", columnList = "archived")
})
public class Project extends BaseEntity {

    @NotBlank(message = "Project name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    // store email from JWT instead of userId
    @Column(nullable = false)
    private String ownerEmail;

    @Column(length = 50)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectVisibility visibility = ProjectVisibility.PRIVATE;

    @Column(nullable = false)
    private Boolean archived = false;

    @Column(nullable = false)
    private Long starCount = 0L;

    @Column(nullable = false)
    private Long forkCount = 0L;

    // allow null for original project
    @Column(nullable = true)
    private Long parentProjectId;

    @Column(nullable = false)
    private String defaultBranch = "main";
}