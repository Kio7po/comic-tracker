package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.kio7po.comic_tracker.adapter.rest.security.JwtDecoderConfig;
import com.github.kio7po.comic_tracker.adapter.rest.security.SecurityConfig;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.UserRole;
import com.github.kio7po.comic_tracker.domain.service.UserService;

// Imports the real SecurityConfig/JwtDecoderConfig rather than faking a permissive chain, so
// this test reflects actual authorization behavior (/me authenticated) instead of drifting
// from it silently if those rules ever change.
@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class })
class UserControllerTest {

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
    void meReturnsAuthenticatedUser() throws Exception {
        when(userService.findById(1L)).thenReturn(user());

        mockMvc.perform(get("/api/users/me").with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void meReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    @Test
    void updateProfileReturnsUpdatedUserBody() throws Exception {
        User updated = user();
        updated.setDisplayName("New Name");
        updated.setBiography("New bio");
        updated.setPictureUrl("https://example.com/pic.png");
        updated.setLocale("en-US");
        when(userService.updateProfile(1L, "New Name", "New bio", "https://example.com/pic.png", "en-US"))
                .thenReturn(updated);

        mockMvc.perform(put("/api/users/me")
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"displayName":"New Name","biography":"New bio","pictureUrl":"https://example.com/pic.png","locale":"en-US"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("New Name"))
                .andExpect(jsonPath("$.biography").value("New bio"))
                .andExpect(jsonPath("$.pictureUrl").value("https://example.com/pic.png"))
                .andExpect(jsonPath("$.locale").value("en-US"));
    }

    @Test
    void updateProfileReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(put("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"displayName":"New Name"}
                        """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userService);
    }

    // Proveedor de casos de prueba para JSONs que deben devolver BadRequest
    private static Stream<Arguments> invalidUpdateProfileRequests() {
        return Stream.of(
                Arguments.of("blank displayName", """
                        {"displayName":""}
                        """),
                Arguments.of("blank biography", """
                        {"displayName":"New Name","biography":"   "}
                        """),
                Arguments.of("invalid pictureUrl", """
                        {"displayName":"New Name","pictureUrl":"not-a-url"}
                        """),
                Arguments.of("invalid locale", """
                        {"displayName":"New Name","locale":"not-a-locale"}
                        """));
    }

    // Usa el primer argumento como display name del test
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidUpdateProfileRequests")
    void updateProfileReturnsBadRequestOnInvalidRequest(String caseName, String json) throws Exception {
        mockMvc.perform(put("/api/users/me")
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

}
