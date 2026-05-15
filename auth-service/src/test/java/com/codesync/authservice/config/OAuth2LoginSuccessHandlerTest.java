package com.codesync.authservice.config;

import com.codesync.authservice.entity.User;
import com.codesync.authservice.repository.UserRepository;
import com.codesync.authservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for OAuth2LoginSuccessHandler.
 *
 * Covers behaviour introduced / fixed across three rounds of changes:
 *   Round 1 – Initial implementation (find-or-create, JWT redirect).
 *   Round 2 – Username-collision suffix, null-email fallback, avatar fetch.
 *   Round 3 – Scope fix (user:email now granted → real email returned by GitHub),
 *             forward-headers-strategy:none (server config – not unit-testable),
 *             gateway RemoveRequestHeader filters (gateway config – not unit-testable).
 */
@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private OAuth2AuthenticationToken authToken;
    @Mock private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2LoginSuccessHandler handler;

    private static final String JWT = "jwt-token-abc";

    @BeforeEach
    void commonStubs() {
        lenient().when(authToken.getPrincipal()).thenReturn(oAuth2User);
        lenient().when(jwtUtil.generateToken(anyString(), anyString(), anyString())).thenReturn(JWT);
    }

    // ══════════════════════════════════════════════════════════════════
    // GitHub — NEW USER scenarios
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GitHub — new user signup")
    class GitHubNewUser {

        /**
         * Fix Round 3: scope now includes user:email, so GitHub returns the real
         * email address instead of null. This must be used as the canonical email.
         */
        @Test
        @DisplayName("public email (user:email scope granted) → real email stored")
        void withPublicEmail_storesRealEmail() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("dev@example.com");   // scope grants this
            when(oAuth2User.getAttribute("name")).thenReturn("Dev User");
            when(oAuth2User.getAttribute("login")).thenReturn("devuser");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn("https://avatars.githubusercontent.com/u/1");

            when(userRepository.findByEmail("dev@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("devuser")).thenReturn(Optional.empty());

            User saved = User.builder().email("dev@example.com").role("DEVELOPER")
                    .username("devuser").avatarUrl("https://avatars.githubusercontent.com/u/1").build();
            when(userRepository.save(any(User.class))).thenReturn(saved);

            handler.onAuthenticationSuccess(request, response, authToken);

            // Verify redirect to Angular callback with JWT
            verify(response).sendRedirect("http://localhost:4200/oauth-callback?token=" + JWT);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User created = captor.getValue();

            // Core identity fields
            assertEquals("dev@example.com", created.getEmail());
            assertEquals("GITHUB", created.getProvider());
            assertEquals("devuser", created.getUsername());          // uses GitHub login, not display name
            assertEquals("Dev User", created.getFullName());
            assertEquals("https://avatars.githubusercontent.com/u/1", created.getAvatarUrl()); // avatar fetched
            assertEquals("DEVELOPER", created.getRole());
            assertTrue(created.isActive());
            assertNotNull(created.getCreatedAt());                   // timestamp set
        }

        /**
         * Fix Round 2: users whose GitHub email is set to Private return null
         * from the email attribute. The handler must fall back to login@github.com
         * (GitHub login is globally unique so this fallback is safe).
         *
         * Fix Round 3: scope change means real email is returned when not private.
         * This test verifies the null-path still works correctly after scope fix.
         */
        @Test
        @DisplayName("private email (null from GitHub) → synthetic login@github.com used")
        void withPrivateEmail_usesSyntheticEmail() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn(null);       // user set email to private
            when(oAuth2User.getAttribute("name")).thenReturn("Ghost");
            when(oAuth2User.getAttribute("login")).thenReturn("ghostuser");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);

            when(userRepository.findByEmail("ghostuser@github.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("ghostuser")).thenReturn(Optional.empty());

            User saved = User.builder().email("ghostuser@github.com").role("DEVELOPER")
                    .avatarUrl(null).build();
            when(userRepository.save(any(User.class))).thenReturn(saved);

            handler.onAuthenticationSuccess(request, response, authToken);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User created = captor.getValue();

            assertEquals("ghostuser@github.com", created.getEmail(),
                    "Synthetic email must use the globally-unique GitHub login handle");
            assertEquals("GITHUB", created.getProvider());
            assertEquals("ghostuser", created.getUsername());
        }

        /**
         * Fix Round 2: username field is UNIQUE in DB. If the GitHub login handle
         * is already taken (e.g. two different OAuth providers both have 'devuser'),
         * the handler must append numeric suffixes until a free slot is found.
         */
        @Test
        @DisplayName("username collision → numeric suffix until free slot found")
        void usernameCollision_appendsSuffix() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("new@example.com");
            when(oAuth2User.getAttribute("name")).thenReturn("Dev");
            when(oAuth2User.getAttribute("login")).thenReturn("devuser");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);

            when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
            // devuser taken, devuser1 taken, devuser2 free
            when(userRepository.findByUsername("devuser"))
                    .thenReturn(Optional.of(User.builder().username("devuser").build()));
            when(userRepository.findByUsername("devuser1"))
                    .thenReturn(Optional.of(User.builder().username("devuser1").build()));
            when(userRepository.findByUsername("devuser2")).thenReturn(Optional.empty());

            User saved = User.builder().email("new@example.com").role("DEVELOPER")
                    .username("devuser2").avatarUrl(null).build();
            when(userRepository.save(any(User.class))).thenReturn(saved);

            handler.onAuthenticationSuccess(request, response, authToken);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("devuser2", captor.getValue().getUsername(),
                    "Must skip taken handles and use the first free one");
        }

        @Test
        @DisplayName("null display name → falls back to GitHub login as fullName")
        void nullDisplayName_usesLoginAsFullName() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("noname@example.com");
            when(oAuth2User.getAttribute("name")).thenReturn(null);        // no display name set
            when(oAuth2User.getAttribute("login")).thenReturn("noname");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);

            when(userRepository.findByEmail("noname@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("noname")).thenReturn(Optional.empty());

            User saved = User.builder().email("noname@example.com").role("DEVELOPER")
                    .username("noname").fullName("noname").avatarUrl(null).build();
            when(userRepository.save(any(User.class))).thenReturn(saved);

            handler.onAuthenticationSuccess(request, response, authToken);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("noname", captor.getValue().getFullName(),
                    "fullName must fall back to the login handle when name is null");
        }

        /**
         * Fix Round 2: avatar_url is now fetched from GitHub and stored at signup.
         */
        @Test
        @DisplayName("avatar_url fetched from GitHub and persisted on new user")
        void avatarFetchedFromGitHub() throws Exception {
            String avatarUrl = "https://avatars.githubusercontent.com/u/99";
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("pic@example.com");
            when(oAuth2User.getAttribute("name")).thenReturn("Pic User");
            when(oAuth2User.getAttribute("login")).thenReturn("picuser");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(avatarUrl);

            when(userRepository.findByEmail("pic@example.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("picuser")).thenReturn(Optional.empty());

            User saved = User.builder().email("pic@example.com").role("DEVELOPER")
                    .username("picuser").avatarUrl(avatarUrl).build();
            when(userRepository.save(any(User.class))).thenReturn(saved);

            handler.onAuthenticationSuccess(request, response, authToken);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals(avatarUrl, captor.getValue().getAvatarUrl());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // GitHub — EXISTING USER (return login) scenarios
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GitHub — existing user login")
    class GitHubExistingUser {

        @Test
        @DisplayName("existing user → no duplicate DB insert")
        void existingUser_noNewUserCreated() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("existing@example.com");
            when(oAuth2User.getAttribute("name")).thenReturn("Existing");
            when(oAuth2User.getAttribute("login")).thenReturn("existinguser");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn("https://avatars.githubusercontent.com/same");

            User existingUser = User.builder()
                    .email("existing@example.com").role("DEVELOPER")
                    .avatarUrl("https://avatars.githubusercontent.com/same").build();
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

            handler.onAuthenticationSuccess(request, response, authToken);

            // Avatar is the same → no save at all
            verify(userRepository, never()).save(any());
            verify(response).sendRedirect(contains("/oauth-callback?token=" + JWT));
        }

        @Test
        @DisplayName("existing user with updated avatar → avatar saved, no new user row")
        void existingUser_avatarChanged_savedOnce() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("existing@example.com");
            when(oAuth2User.getAttribute("name")).thenReturn("Existing");
            when(oAuth2User.getAttribute("login")).thenReturn("existinguser");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn("https://avatars.githubusercontent.com/NEW");

            User existingUser = User.builder()
                    .email("existing@example.com").role("DEVELOPER")
                    .avatarUrl("https://avatars.githubusercontent.com/OLD").build();
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));
            when(userRepository.save(existingUser)).thenReturn(existingUser);

            handler.onAuthenticationSuccess(request, response, authToken);

            // Exactly one save — avatar update, not a new user
            verify(userRepository, times(1)).save(existingUser);
            assertEquals("https://avatars.githubusercontent.com/NEW", existingUser.getAvatarUrl());
            verify(response).sendRedirect(contains("/oauth-callback?token="));
        }

        @Test
        @DisplayName("existing user with null GitHub avatar → no avatar update save")
        void existingUser_nullAvatar_noSave() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("existing@example.com");
            when(oAuth2User.getAttribute("name")).thenReturn("Existing");
            when(oAuth2User.getAttribute("login")).thenReturn("existinguser");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);  // GitHub returned no avatar

            User existingUser = User.builder()
                    .email("existing@example.com").role("DEVELOPER")
                    .avatarUrl("https://avatars.githubusercontent.com/prev").build();
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

            handler.onAuthenticationSuccess(request, response, authToken);

            // avatarFinal is null → condition (avatarFinal != null && ...) is false → no save
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("JWT is generated from stored user email and role")
        void jwtUsesStoredUserEmailAndRole() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("user@example.com");
            when(oAuth2User.getAttribute("name")).thenReturn("User");
            when(oAuth2User.getAttribute("login")).thenReturn("user");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);

            User existingUser = User.builder()
                    .email("user@example.com").role("ADMIN")
                    .avatarUrl(null).build();
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));

            handler.onAuthenticationSuccess(request, response, authToken);

            // JWT must use stored role (ADMIN) and plan
            verify(jwtUtil).generateToken(eq("user@example.com"), eq("ADMIN"), anyString());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Google — scenarios
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Google OAuth")
    class GoogleOAuth {

        @Test
        @DisplayName("new Google user → created with picture from 'picture' attribute")
        void newGoogleUser_createdWithPicture() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("google");
            when(oAuth2User.getAttribute("email")).thenReturn("user@gmail.com");
            when(oAuth2User.getAttribute("name")).thenReturn("Gmail User");
            when(oAuth2User.getAttribute("login")).thenReturn(null);       // Google has no 'login'
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);  // GitHub field
            when(oAuth2User.getAttribute("picture")).thenReturn("https://lh3.googleusercontent.com/photo");

            when(userRepository.findByEmail("user@gmail.com")).thenReturn(Optional.empty());
            when(userRepository.findByUsername("user")).thenReturn(Optional.empty()); // email prefix

            User saved = User.builder().email("user@gmail.com").role("DEVELOPER")
                    .username("user").avatarUrl("https://lh3.googleusercontent.com/photo").build();
            when(userRepository.save(any(User.class))).thenReturn(saved);

            handler.onAuthenticationSuccess(request, response, authToken);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User created = captor.getValue();

            assertEquals("user@gmail.com", created.getEmail());
            assertEquals("GOOGLE", created.getProvider());
            // avatar comes from Google's 'picture' field, not 'avatar_url'
            assertEquals("https://lh3.googleusercontent.com/photo", created.getAvatarUrl());
        }

        @Test
        @DisplayName("Google user with username collision → numeric suffix applied")
        void googleUser_usernameCollision_suffix() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("google");
            when(oAuth2User.getAttribute("email")).thenReturn("john@gmail.com");
            when(oAuth2User.getAttribute("name")).thenReturn("John");
            when(oAuth2User.getAttribute("login")).thenReturn(null);
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);
            when(oAuth2User.getAttribute("picture")).thenReturn(null);

            when(userRepository.findByEmail("john@gmail.com")).thenReturn(Optional.empty());
            // "john" is taken (email prefix), "john1" is free
            when(userRepository.findByUsername("john"))
                    .thenReturn(Optional.of(User.builder().username("john").build()));
            when(userRepository.findByUsername("john1")).thenReturn(Optional.empty());

            User saved = User.builder().email("john@gmail.com").role("DEVELOPER")
                    .username("john1").avatarUrl(null).build();
            when(userRepository.save(any(User.class))).thenReturn(saved);

            handler.onAuthenticationSuccess(request, response, authToken);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertEquals("john1", captor.getValue().getUsername());
        }

        @Test
        @DisplayName("returning Google user → no duplicate insert")
        void returningGoogleUser_noDuplicate() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("google");
            when(oAuth2User.getAttribute("email")).thenReturn("returning@gmail.com");
            when(oAuth2User.getAttribute("name")).thenReturn("Returning");
            when(oAuth2User.getAttribute("login")).thenReturn(null);
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);
            when(oAuth2User.getAttribute("picture")).thenReturn("https://lh3.googleusercontent.com/photo");

            User existingUser = User.builder()
                    .email("returning@gmail.com").role("DEVELOPER")
                    .avatarUrl("https://lh3.googleusercontent.com/photo").build();
            when(userRepository.findByEmail("returning@gmail.com")).thenReturn(Optional.of(existingUser));

            handler.onAuthenticationSuccess(request, response, authToken);

            // avatar unchanged → no save
            verify(userRepository, never()).save(any());
            verify(response).sendRedirect(contains("/oauth-callback?token="));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Redirect URL correctness
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Redirect URL")
    class RedirectUrl {

        /**
         * Fix Round 3: The gateway now strips X-Forwarded-Host headers and
         * auth-service is configured with forward-headers-strategy:none.
         * The result is that {baseUrl} resolves to http://localhost:8081 and the
         * redirect back to Angular is always http://localhost:4200/oauth-callback.
         * This test verifies the handler emits the correct hardcoded Angular URL.
         */
        @Test
        @DisplayName("redirects to Angular oauth-callback with JWT token in query param")
        void redirectContainsCorrectUrlAndToken() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("redir@example.com");
            when(oAuth2User.getAttribute("name")).thenReturn("Redir");
            when(oAuth2User.getAttribute("login")).thenReturn("rediruser");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);

            when(userRepository.findByEmail("redir@example.com"))
                    .thenReturn(Optional.of(User.builder().email("redir@example.com")
                            .role("DEVELOPER").avatarUrl(null).build()));

            handler.onAuthenticationSuccess(request, response, authToken);

            // Must redirect to Angular frontend (not auth-service), with token as ?token= param
            verify(response).sendRedirect("http://localhost:4200/oauth-callback?token=" + JWT);
        }

        @Test
        @DisplayName("JWT is generated with correct email and role before redirect")
        void jwtGeneratedWithCorrectClaims() throws Exception {
            when(authToken.getAuthorizedClientRegistrationId()).thenReturn("github");
            when(oAuth2User.getAttribute("email")).thenReturn("claims@example.com");
            when(oAuth2User.getAttribute("name")).thenReturn("Claims User");
            when(oAuth2User.getAttribute("login")).thenReturn("claimsuser");
            when(oAuth2User.getAttribute("avatar_url")).thenReturn(null);

            when(userRepository.findByEmail("claims@example.com"))
                    .thenReturn(Optional.of(User.builder().email("claims@example.com")
                            .role("DEVELOPER").avatarUrl(null).build()));

            handler.onAuthenticationSuccess(request, response, authToken);

            verify(jwtUtil).generateToken(eq("claims@example.com"), eq("DEVELOPER"), anyString());
        }
    }
}
