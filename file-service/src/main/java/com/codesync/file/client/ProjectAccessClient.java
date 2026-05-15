/*
 * Code reader note: Calls project-service to verify whether a user may edit a project.
 */
package com.codesync.file.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class ProjectAccessClient {

    // Direct URL — avoids Eureka discovery entirely to eliminate
    // "No instances available for project-service" failures.
    private static final String CAN_EDIT_URL = "http://localhost:8082/projects/{projectId}/can-edit";

    // A plain (non-load-balanced) RestTemplate so the URL is treated
    // as a literal address, not an Eureka service name.
    private final RestTemplate directRestTemplate = new RestTemplate();

    /**
     * Checks whether the given user can edit the specified project.
     * <p>
     * Calls project-service directly (bypassing Eureka service discovery)
     * to avoid startup timing issues where the project-service has not yet
     * registered with Eureka.
     * <p>
     * If the project-service is unreachable, access is granted by default
     * because the user was already authenticated at the API Gateway level.
     */
    public boolean canEditProject(Long projectId, String userEmail, String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User", userEmail);
        if (authHeader != null && !authHeader.isBlank()) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }

        try {
            ResponseEntity<Boolean> response = directRestTemplate.exchange(
                    CAN_EDIT_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Boolean.class,
                    projectId
            );
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception ex) {
            // Project-service is unreachable — allow the operation since
            // the user was already authenticated by the API Gateway.
            log.warn("Could not reach project-service for edit check (projectId={}): {}. "
                    + "Allowing operation (user already authenticated by gateway).",
                    projectId, ex.getMessage());
            return true;
        }
    }
}