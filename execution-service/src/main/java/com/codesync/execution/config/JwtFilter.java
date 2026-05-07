/*
 * Code reader note: Authenticates bearer tokens for execution-service requests.
 */
package com.codesync.execution.config;

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
 * Gateway-trusting JWT filter for Execution Service.
 * Reads X-User (email) and X-Role from API Gateway headers and populates
 * the Spring Security context with the appropriate GrantedAuthority.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // The gateway has already validated the JWT; this service receives the
        // authenticated identity as headers.
        String user = request.getHeader("X-User");
        String role = request.getHeader("X-Role");

        if (user != null && !user.isEmpty()) {
            // Attach role information when present so admin endpoints can be
            // protected by normal Spring Security role checks.
            List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            // Execution jobs use this principal as the job owner email.
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
