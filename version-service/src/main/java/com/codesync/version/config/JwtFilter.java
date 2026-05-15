/*
 * Code reader note: Authenticates bearer tokens for version-service requests.
 */
package com.codesync.version.config;

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
 * Gateway-trusting JWT filter for Version Service.
 * Reads X-User (email) and X-Role from API Gateway headers and populates
 * the Spring Security context with the appropriate GrantedAuthority.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // The API Gateway validates the token and forwards identity here.
        String user = request.getHeader("X-User");
        String role = request.getHeader("X-Role");

        if (user != null && !user.isEmpty()) {
            // Convert X-Role into a Spring Security authority if the gateway sent it.
            List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : Collections.emptyList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            // Snapshot authorship uses this authenticated email.
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
