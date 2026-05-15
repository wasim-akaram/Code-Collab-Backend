/*
 * Code reader note: Reads incoming Bearer tokens, rejects revoked tokens, and stores the
 * authenticated email and role in Spring Security.
 * Annotations used: @RequiredArgsConstructor injects JwtUtil and RedisTemplate, and
 * @Override marks the filter hook implemented from OncePerRequestFilter.
 */
package com.codesync.authservice.config;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.codesync.authservice.util.JwtUtil;

import lombok.RequiredArgsConstructor;

/**
 * JWT filter for the auth-service.
 *
 * Public paths (no token needed):
 *   POST /auth/register, /auth/verify-and-register, /auth/login,
 *   /auth/send-otp, /auth/verify-otp, /auth/refresh,
 *   GET  /auth/search, /auth/users/{id}, /auth/health,
 *   POST /auth/forgot-password, /auth/reset-password, /auth/logout,
 *   /oauth2/**, /login/oauth2/**, /v3/api-docs/**, /swagger-ui/**
 *
 * Protected paths (Bearer token required):
 *   GET  /auth/profile
 *   PUT  /auth/profile
 *   PUT  /auth/password
 *   DELETE /auth/deactivate
 */
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    /** Paths that do NOT require a JWT. Checked by prefix/exact match. */
    private static final List<String> PUBLIC_PATHS = List.of(
        "/auth/register",
        "/auth/email-exists",
        "/auth/verify-and-register",
        "/auth/login",
        "/auth/send-otp",
        "/auth/verify-otp",
        "/auth/refresh",
        "/auth/forgot-password",
        "/auth/reset-password",
        "/auth/search",
        "/auth/users/",
        "/auth/health",
        "/auth/logout",
        "/oauth2/",
        "/login/oauth2/",
        "/v3/api-docs",
        "/swagger-ui",
        "/swagger-ui.html"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // Allow public paths to pass through without a token.
        // This keeps signup/login/password-reset reachable before authentication.
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // All other /auth/** paths require a valid Bearer token.
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // ── Blacklist check ──────────────────────────────────────────
                // If this token was explicitly logged out, reject immediately.
                if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("text/plain");
                    response.getWriter().write("Token has been revoked. Please log in again.");
                    return;
                }
                // ─────────────────────────────────────────────────────────────

                String email = jwtUtil.extractEmail(token);
                String role  = jwtUtil.extractRole(token);
                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Spring Security expects roles to use the ROLE_ prefix when
                    // hasRole(...) checks are used in configuration.
                    List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
                            (role != null && !role.isBlank())
                            ? java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))
                            : java.util.Collections.emptyList();
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // From this point onward, controllers can read the authenticated
                    // email through @AuthenticationPrincipal or the SecurityContext.
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Invalid token — let Spring Security reject at authorizeHttpRequests level.
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        if (path == null) return false;
        for (String pub : PUBLIC_PATHS) {
            // Prefix matching allows entries like /auth/users/ to cover
            // /auth/users/{id}.
            if (path.startsWith(pub)) return true;
        }
        return false;
    }
}
