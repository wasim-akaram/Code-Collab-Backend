/*
 * Code reader note: Represents a user membership and role assignment inside a project.
 */
package com.codesync.project.entity;

import com.codesync.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ProjectMember entity — tracks which users have access to a private project.
 * Uses userEmail (String) to match the API Gateway X-User email identity model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"projectId", "userEmail"})
}, indexes = {
    @Index(name = "idx_pm_project_id",  columnList = "projectId"),
    @Index(name = "idx_pm_user_email",  columnList = "userEmail")
})
public class ProjectMember extends BaseEntity {

    @Column(nullable = false)
    private Long projectId;

    /** Email of the member — matches the X-User header from the API Gateway. */
    @Column(nullable = false)
    private String userEmail;

    /** Role of the member: OWNER | EDITOR | VIEWER */
    @Column(nullable = false, length = 20)
    private String role;
}
