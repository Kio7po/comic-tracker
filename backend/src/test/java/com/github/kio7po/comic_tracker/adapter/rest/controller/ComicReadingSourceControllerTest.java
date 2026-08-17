package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.kio7po.comic_tracker.adapter.rest.exception.ProblemType;
import com.github.kio7po.comic_tracker.adapter.rest.security.JwtDecoderConfig;
import com.github.kio7po.comic_tracker.adapter.rest.security.SecurityConfig;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceService;

// Imports the real SecurityConfig so this test reflects actual authorization behavior
// (GET public, approve/reject ADMIN-only) instead of drifting from it silently if those rules ever change.
@WebMvcTest(ComicReadingSourceController.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class })
class ComicReadingSourceControllerTest {

    private static final Long SOURCE_ID = 1L;
    // Matches the "1" subject set on every authenticated jwt() request below.
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComicReadingSourceService comicReadingSourceService;

    private static ComicReadingSource source(Long id, String name, ComicReadingSourceStatus status) {
        ComicReadingSource source = new ComicReadingSource();
        source.setId(id);
        source.setSlug(name.toLowerCase());
        source.setName(name);
        source.setUrl("https://" + name.toLowerCase() + ".org");
        source.setStatus(status);
        source.setContributedBy(contributor());
        return source;
    }

    private static User contributor() {
        User user = new User();
        user.setUsername("contributoruser");
        return user;
    }

    @Test
    void findByStatusInReturnsSourcesMappedToDto() throws Exception {
        when(comicReadingSourceService.findByStatusIn(List.of(ComicReadingSourceStatus.PENDING),
                ComicReadingSourceSortField.NAME, SortDirection.ASC))
                .thenReturn(List.of(source(1L, "MangaDex", ComicReadingSourceStatus.PENDING)));

        mockMvc.perform(get("/api/reading-sources").param("statuses", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].slug").value("mangadex"))
                .andExpect(jsonPath("$[0].name").value("MangaDex"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void findByStatusInPassesSortParamsThrough() throws Exception {
        when(comicReadingSourceService.findByStatusIn(List.of(ComicReadingSourceStatus.APPROVED,
                ComicReadingSourceStatus.PENDING), ComicReadingSourceSortField.CREATED_AT, SortDirection.DESC))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/reading-sources")
                .param("statuses", "APPROVED", "PENDING")
                .param("sortBy", "CREATED_AT")
                .param("direction", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findByStatusInIsPublic() throws Exception {
        when(comicReadingSourceService.findByStatusIn(List.of(ComicReadingSourceStatus.PENDING),
                ComicReadingSourceSortField.NAME, SortDirection.ASC)).thenReturn(List.of());

        mockMvc.perform(get("/api/reading-sources").param("statuses", "PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    void findForModerationByStatusInRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/moderation/reading-sources").param("statuses", "PENDING"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findForModerationByStatusInReturnsForbiddenForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/moderation/reading-sources")
                .param("statuses", "PENDING")
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void findForModerationByStatusInReturnsSourcesWithContributorForAdminUser() throws Exception {
        when(comicReadingSourceService.findByStatusIn(List.of(ComicReadingSourceStatus.PENDING),
                ComicReadingSourceSortField.NAME, SortDirection.ASC))
                .thenReturn(List.of(source(1L, "MangaDex", ComicReadingSourceStatus.PENDING)));

        mockMvc.perform(get("/api/moderation/reading-sources")
                .param("statuses", "PENDING")
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source.id").value(1))
                .andExpect(jsonPath("$[0].source.name").value("MangaDex"))
                .andExpect(jsonPath("$[0].contributedBy.username").value("contributoruser"));
    }

    @Test
    void approveRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/reading-sources/{id}/approve", SOURCE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveReturnsForbiddenForNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/reading-sources/{id}/approve", SOURCE_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveReturnsOkForAdminUser() throws Exception {
        when(comicReadingSourceService.approve(SOURCE_ID, USER_ID))
                .thenReturn(source(SOURCE_ID, "MangaDex", ComicReadingSourceStatus.APPROVED));

        mockMvc.perform(post("/api/reading-sources/{id}/approve", SOURCE_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approveReturnsConflictWhenSourceAlreadyReviewed() throws Exception {
        when(comicReadingSourceService.approve(SOURCE_ID, USER_ID))
                .thenThrow(new ComicReadingSourceAlreadyReviewedException(SOURCE_ID, ComicReadingSourceStatus.APPROVED));

        mockMvc.perform(post("/api/reading-sources/{id}/approve", SOURCE_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_SOURCE_ALREADY_REVIEWED));
    }

    @Test
    void rejectRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/reading-sources/{id}/reject", SOURCE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectReturnsForbiddenForNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/reading-sources/{id}/reject", SOURCE_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectReturnsOkForAdminUser() throws Exception {
        when(comicReadingSourceService.reject(SOURCE_ID, USER_ID))
                .thenReturn(source(SOURCE_ID, "MangaDex", ComicReadingSourceStatus.REJECTED));

        mockMvc.perform(post("/api/reading-sources/{id}/reject", SOURCE_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rejectReturnsConflictWhenSourceAlreadyReviewed() throws Exception {
        when(comicReadingSourceService.reject(SOURCE_ID, USER_ID))
                .thenThrow(new ComicReadingSourceAlreadyReviewedException(SOURCE_ID, ComicReadingSourceStatus.REJECTED));

        mockMvc.perform(post("/api/reading-sources/{id}/reject", SOURCE_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_SOURCE_ALREADY_REVIEWED));
    }

}