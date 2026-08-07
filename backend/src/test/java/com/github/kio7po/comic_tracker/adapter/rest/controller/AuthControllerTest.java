package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.kio7po.comic_tracker.adapter.rest.exception.ProblemType;
import com.github.kio7po.comic_tracker.adapter.rest.security.JwtDecoderConfig;
import com.github.kio7po.comic_tracker.adapter.rest.security.SecurityConfig;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.UserRole;
import com.github.kio7po.comic_tracker.domain.exceptions.EmailAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.InvalidCredentialsException;
import com.github.kio7po.comic_tracker.domain.exceptions.UsernameAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.service.TokenPair;
import com.github.kio7po.comic_tracker.domain.service.UserService;

import jakarta.servlet.http.Cookie;

// Imports the real SecurityConfig/JwtDecoderConfig rather than faking a permissive chain, so
// this test reflects actual authorization behavior (register/login/refresh/logout public,
// /me authenticated) instead of drifting from it silently if those rules ever change.
@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private static User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setDisplayName("Test User");
        user.setRole(UserRole.USER);
        return user;
    }

    @Test
    void registerReturnsCreatedWithUserBody() throws Exception {
        when(userService.register("testuser", "testuser@example.com", "password123", "Test User"))
                .thenReturn(user());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"testuser","email":"testuser@example.com","password":"password123","displayName":"Test User"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void registerReturnsBadRequestOnBlankUsername() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"","email":"testuser@example.com","password":"password123","displayName":"Test User"}
                        """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void registerReturnsConflictWhenUsernameAlreadyExists() throws Exception {
        when(userService.register(any(), any(), any(), any()))
                .thenThrow(new UsernameAlreadyExistsException("testuser"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"testuser","email":"testuser@example.com","password":"password123","displayName":"Test User"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.USERNAME_ALREADY_EXISTS));
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        when(userService.register(any(), any(), any(), any()))
                .thenThrow(new EmailAlreadyExistsException("testuser@example.com"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"testuser","email":"testuser@example.com","password":"password123","displayName":"Test User"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void loginReturnsTokenBodyAndSetsRefreshCookie() throws Exception {
        when(userService.login("testuser", "password123")).thenReturn(new TokenPair("access-token", "refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"usernameOrEmail":"testuser","password":"password123"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access-token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(cookie().value("refresh_token", "refresh-token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/auth"));
    }

    @Test
    void loginReturnsUnauthorizedOnInvalidCredentials() throws Exception {
        when(userService.login(any(), any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"usernameOrEmail":"testuser","password":"wrong"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value(ProblemType.INVALID_CREDENTIALS));
    }

    @Test
    void meReturnsAuthenticatedUser() throws Exception {
        when(userService.findById(1L)).thenReturn(user());

        mockMvc.perform(get("/api/auth/me").with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void logoutClearsTheRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", "some-token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refresh_token", 0));

        verify(userService).logout("some-token");
    }

}
