/*
 * Code reader note: Authenticates bearer tokens for project-service requests and exposes the email to Spring Security.
 */
package com.codesync.project.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT Filter (Gateway-trusting version).
 * Trusts the X-User and X-Role headers set by the API Gateway.
 * Does NOT validate the JWT — that is done once at the gateway.
 */
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();
        if (path == null) path = request.getRequestURI();
        if (path == null) path = "";

        // Allow Swagger through without auth
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        // The gateway already validated the JWT, so this service trusts the
        // identity headers it receives from the gateway.
        String user = request.getHeader("X-User");
        String role = request.getHeader("X-Role");

        if (user != null && !user.isEmpty()) {
            // Convert the forwarded role into Spring Security's authority format.
            List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            // Service methods later read this principal as the current user email.
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
