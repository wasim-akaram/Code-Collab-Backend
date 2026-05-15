package com.codesync.payment.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT Filter (Gateway-trusting version).
 * Trusts the X-User, X-Role, and X-User-Plan headers set by the API Gateway.
 * Does NOT validate the JWT — that is done once at the gateway.
 *
 * NOTE: This class is NOT a @Component. It is instantiated manually in
 * SecurityConfig to avoid the dual-registration bug where Spring Boot
 * registers the filter as both a servlet filter and a security filter.
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

        // Allow Swagger and actuator through without auth
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // The gateway already validated the JWT, so this service trusts the
        // identity headers it receives from the gateway.
        String user = request.getHeader("X-User");
        String role = request.getHeader("X-Role");
        String plan = request.getHeader("X-User-Plan");

        if (user != null && !user.isEmpty()) {
            List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of(new SimpleGrantedAuthority("ROLE_USER"));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);

            // Store the user's plan in authentication details so service methods
            // can check it via SecurityContextHolder.
            Map<String, String> details = new HashMap<>();
            details.put("plan", plan != null ? plan : "FREE");
            auth.setDetails(details);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user '{}' via gateway X-User header (plan={})", user, plan);
        } else {
            log.debug("No X-User header on request {} {} — continuing unauthenticated", request.getMethod(), path);
        }

        filterChain.doFilter(request, response);
    }
}
