/*
 * Code reader note: Authenticates bearer tokens for collab-service HTTP requests.
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
import java.util.List;

/**
 * Gateway-trusting JWT filter for Collab Service.
 * Reads X-User (email) and X-Role from API Gateway headers and populates
 * the Spring Security context with the appropriate GrantedAuthority.
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

        if (user != null && !user.isEmpty()) {
            // Roles become GrantedAuthority values so @PreAuthorize/hasRole checks
            // can work normally.
            List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            // Controllers and services now see the email as the authenticated user.
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
