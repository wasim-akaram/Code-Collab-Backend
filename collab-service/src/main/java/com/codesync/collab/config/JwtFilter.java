/*
 * Code reader note: Reads gateway-forwarded identity headers and stores the
 * authenticated user and role in the Spring Security context for collab-service.
 * Annotations used: @Component registers the filter as a Spring bean, and
 * @Override marks the OncePerRequestFilter hook implementation.
 */
package com.codesync.collab.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gateway-trusting JWT filter for Collab Service.
 * Reads X-User (email), X-Role, and X-User-Plan from API Gateway headers
 * and populates the Spring Security context with the appropriate GrantedAuthority.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // The API Gateway validates the token first and forwards trusted identity
        // headers to this service.
        String user = request.getHeader("X-User");
        String role = request.getHeader("X-Role");
        String plan = request.getHeader("X-User-Plan");

        if (user != null && !user.isEmpty()) {
            // Roles become GrantedAuthority values so @PreAuthorize/hasRole checks
            // can work normally.
            List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            // Store the user's plan for session participant limit enforcement.
            Map<String, String> details = new HashMap<>();
            details.put("plan", plan != null ? plan : "FREE");
            auth.setDetails(details);

            // Controllers and services now see the email as the authenticated user.
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
