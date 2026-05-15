/*
 * Code reader note: Handles successful OAuth login, creates or finds the local user,
 * issues a JWT, and redirects back to the frontend.
 * Annotations used: @Slf4j provides the logger, @Component registers the handler in
 * Spring, @RequiredArgsConstructor injects the repository and JWT helper, and
 * @Override marks the AuthenticationSuccessHandler callback implementation.
 */
package com.codesync.authservice.config;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.codesync.authservice.entity.User;
import com.codesync.authservice.repository.UserRepository;
import com.codesync.authservice.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /** Base URL of the Angular frontend — used for the post-OAuth redirect. */
    private static final String FRONTEND_URL = "http://localhost:4200";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        // Spring tells us which OAuth registration completed, so we can handle
        // provider-specific attributes below.
        String provider = oauthToken.getAuthorizedClientRegistrationId().toUpperCase(); // "GITHUB" or "GOOGLE"

        // ── 1. Resolve email ──────────────────────────────────────────────────
        // GitHub users may have their email set to private; fall back to a
        // deterministic synthetic address derived from their unique login handle.
        String email = oAuth2User.getAttribute("email");
        String login  = oAuth2User.getAttribute("login"); // GitHub username (unique)

        if (email == null && "GITHUB".equals(provider)) {
            // GitHub's login is globally unique, so this is a safe fallback.
            email = login + "@github.com";
            log.debug("GitHub user '{}' has no public email — using synthetic address '{}'", login, email);
        }

        // ── 2. Resolve display name ───────────────────────────────────────────
        String name = oAuth2User.getAttribute("name");
        if (name == null || name.isBlank()) {
            name = login; // GitHub login is always non-null
        }

        // ── 3. Resolve avatar ─────────────────────────────────────────────────
        // GitHub provides avatar_url; Google provides picture.
        String avatarUrl = oAuth2User.getAttribute("avatar_url");  // GitHub
        if (avatarUrl == null) {
            avatarUrl = oAuth2User.getAttribute("picture");         // Google
        }

        final String userEmail  = email;
        final String userName   = name;
        final String avatarFinal = avatarUrl;
        // A final local variable is required because the lambda below may create
        // a new user after checking the repository.
        final String loginHandle = login != null ? login : email.split("@")[0];

        // ── 4. Find or create user ────────────────────────────────────────────
        User user = userRepository.findByEmail(userEmail).orElseGet(() -> {

            // Generate a unique username: prefer GitHub login, then fall back
            // to a timestamped variant if that handle is already taken.
            String baseUsername = loginHandle;
            String candidate = baseUsername;
            int suffix = 1;
            while (userRepository.findByUsername(candidate).isPresent()) {
                // Keep trying username, username1, username2, ... until no user
                // currently owns the generated handle.
                candidate = baseUsername + suffix;
                suffix++;
            }

            log.info("Creating new OAuth user: email={}, username={}, provider={}", userEmail, candidate, provider);

            User newUser = User.builder()
                    .username(candidate)
                    .email(userEmail)
                    .fullName(userName)
                    .avatarUrl(avatarFinal)
                    .provider(provider)
                    .role("DEVELOPER")
                    .active(true)
                    .createdAt(Instant.now())
                    .build();
            return userRepository.save(newUser);
        });

        // ── 5. Update avatar if it changed (e.g. user changed their GitHub pic) ─
        if (avatarFinal != null && !avatarFinal.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatarFinal);
            userRepository.save(user);
        }

        // ── 6. Issue JWT and redirect to Angular ──────────────────────────────
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole(),
                user.getPlan() != null ? user.getPlan() : "FREE");
        log.info("OAuth login success for {} via {}", user.getEmail(), provider);
        // The frontend callback route reads this token and stores it like a normal
        // email/password login token.
        response.sendRedirect(FRONTEND_URL + "/oauth-callback?token=" + token);
    }
}
