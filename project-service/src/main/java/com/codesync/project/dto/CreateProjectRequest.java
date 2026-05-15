/*
 * Code reader note: Defines the API payload shape used by project-service controllers and clients.
 */
package com.codesync.project.dto;

import com.codesync.common.enums.ProjectVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProjectRequest {
    
    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 100, message = "Project name must be between 1 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    @NotBlank(message = "Language is required")
    @Size(max = 50)
    private String language;
    
    @Builder.Default
    private ProjectVisibility visibility = ProjectVisibility.PRIVATE;
}
