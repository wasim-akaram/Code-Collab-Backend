/*
 * Code reader note: Runs before gateway routing to bypass public paths, validate bearer JWTs on protected paths, and forward the authenticated email as X-User.
 */
package com.codesync.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // The gateway makes its routing decision from the raw request path before
        // the request is proxied to any downstream microservice.
        String path = exchange.getRequest().getURI().getPath();

        // ✅ ALLOW OPTIONS REQUESTS FOR CORS PREFLIGHT
        if (exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // ✅ PUBLIC PATHS — no JWT required at gateway level
        if (isPublicPath(path)) {

            // Even for public paths, if a Bearer token IS present, extract and forward X-User
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    // Optional identity forwarding: public endpoints can still behave
                    // differently for logged-in users when X-User/X-Role are present.
                    Claims claims = parseToken(authHeader.substring(7));
                    String roleVal = claims.get("role", String.class);
                    ServerHttpRequest.Builder rb = exchange.getRequest().mutate()
                            .header("X-User", claims.getSubject())
                            .header(HttpHeaders.AUTHORIZATION, authHeader);
                    if (roleVal != null && !roleVal.isBlank()) {
                        rb.header("X-Role", roleVal);
                    }
                    return chain.filter(exchange.mutate().request(rb.build()).build());
                } catch (Exception ignored) {
                    // Invalid token on a public path — just pass through without X-User
                }
            }

            return chain.filter(exchange);
        }

        // ═══ PROTECTED PATHS — JWT required ═══

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        // ❌ No token → reject
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            // Signature, expiry, and token structure are checked here. Any parsing
            // exception means the request must stop at the gateway.
            claims = parseToken(token);
        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // ✅ Forward user identity AND role to downstream services
        String role = claims.get("role", String.class);
        ServerHttpRequest.Builder reqBuilder = exchange.getRequest()
                .mutate()
                .header("X-User", claims.getSubject())
                .header(HttpHeaders.AUTHORIZATION, authHeader);
        if (role != null && !role.isBlank()) {
            reqBuilder.header("X-Role", role);
        }
        // Downstream services use these trusted headers instead of parsing the
        // token again for every controller method.
        ServerHttpRequest mutatedRequest = reqBuilder.build();

        return chain.filter(
                exchange.mutate().request(mutatedRequest).build()
        );
    }

    /**
     * Determines whether the request path is public (no JWT needed at gateway).
     * Auth-service handles its own fine-grained security for its protected routes.
     */
    private boolean isPublicPath(String path) {
        return
            // Auth — public endpoints only
            path.startsWith("/auth/register") ||
            path.startsWith("/auth/email-exists") ||
            path.startsWith("/auth/verify-and-register") ||
            path.startsWith("/auth/login") ||
            path.startsWith("/auth/send-otp") ||
            path.startsWith("/auth/verify-otp") ||
            path.startsWith("/auth/refresh") ||
            path.startsWith("/auth/forgot-password") ||
            path.startsWith("/auth/reset-password") ||
            path.startsWith("/auth/search") ||
            path.startsWith("/auth/users/") ||
            path.startsWith("/auth/health") ||
            // OAuth2
            path.startsWith("/oauth2") ||
            path.startsWith("/login/oauth2") ||
            // Public project endpoints
            path.startsWith("/projects/public") ||
            path.startsWith("/projects/trending") ||
            path.startsWith("/projects/search") ||
            // Swagger / API-docs (all services)
            path.contains("/v3/api-docs") ||
            path.contains("/swagger-ui") ||
            path.contains("/swagger-resources") ||
            path.contains("/webjars");
    }

    private Claims parseToken(String token) {
        // The same HMAC secret used by auth-service to create tokens is used here
        // to verify that the token was really issued by this backend.
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
