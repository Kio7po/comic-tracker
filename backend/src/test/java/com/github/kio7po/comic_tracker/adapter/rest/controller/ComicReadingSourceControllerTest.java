package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.github.kio7po.comic_tracker.adapter.rest.exception.ProblemType;
import com.github.kio7po.comic_tracker.adapter.rest.security.JwtDecoderConfig;
import com.github.kio7po.comic_tracker.adapter.rest.security.SecurityConfig;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotApprovedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotSupportedException;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSearchListing;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceService;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncReport;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncReport.SkipReason;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncReport.SkippedSource;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncReport.SyncedSource;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncService;
import com.github.kio7po.comic_tracker.domain.service.DiscoveryOutcome;

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
    @MockitoBean
    private ComicReadingSourceSyncService comicReadingSourceSyncService;

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
    void searchRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/reading-sources/{id}/search", SOURCE_ID).param("keywords", "berserk"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchReturnsTheListingMappedToDto() throws Exception {
        when(comicReadingSourceService.search(SOURCE_ID, "berserk")).thenReturn(new ComicReadingSearchListing(
                "suwayomi", List.of(new ComicReadingSearchResult("7", "Berserk", null),
                        new ComicReadingSearchResult(null, "Berserk Deluxe", "https://mangadex.org/title/9"))));

        mockMvc.perform(get("/api/reading-sources/{id}/search", SOURCE_ID)
                .param("keywords", "berserk")
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerKey").value("suwayomi"))
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].externalId").value("7"))
                .andExpect(jsonPath("$.results[0].title").value("Berserk"))
                .andExpect(jsonPath("$.results[0].url").isEmpty())
                .andExpect(jsonPath("$.results[1].externalId").isEmpty())
                .andExpect(jsonPath("$.results[1].url").value("https://mangadex.org/title/9"));
    }

    private static Stream<Arguments> invalidKeywords() {
        return Stream.of(
                Arguments.of("missing keywords", null),
                Arguments.of("empty keywords", ""),
                Arguments.of("blank keywords", "   "),
                Arguments.of("keywords over 255 characters", "a".repeat(256)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidKeywords")
    void searchReturnsBadRequestForInvalidKeywords(String caseName, String keywords) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/reading-sources/{id}/search", SOURCE_ID)
                .with(jwt().jwt(builder -> builder.subject("1")));
        if (keywords != null) {
            request = request.param("keywords", keywords);
        }

        mockMvc.perform(request).andExpect(status().isBadRequest());
    }

    @Test
    void searchReturnsNotFoundWhenTheSourceDoesNotExist() throws Exception {
        when(comicReadingSourceService.search(SOURCE_ID, "berserk"))
                .thenThrow(new ComicReadingSourceNotFoundException(SOURCE_ID));

        mockMvc.perform(get("/api/reading-sources/{id}/search", SOURCE_ID)
                .param("keywords", "berserk")
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_SOURCE_NOT_FOUND));
    }

    @Test
    void searchReturnsConflictWhenTheSourceIsNotApproved() throws Exception {
        when(comicReadingSourceService.search(SOURCE_ID, "berserk"))
                .thenThrow(new ComicReadingSourceNotApprovedException(SOURCE_ID, ComicReadingSourceStatus.PENDING));

        mockMvc.perform(get("/api/reading-sources/{id}/search", SOURCE_ID)
                .param("keywords", "berserk")
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_SOURCE_NOT_APPROVED));
    }

    @Test
    void searchReturnsBadRequestWhenNoProviderSupportsTheSource() throws Exception {
        when(comicReadingSourceService.search(SOURCE_ID, "berserk"))
                .thenThrow(new ComicReadingSourceNotSupportedException(SOURCE_ID));

        mockMvc.perform(get("/api/reading-sources/{id}/search", SOURCE_ID)
                .param("keywords", "berserk")
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_SOURCE_NOT_SUPPORTED));
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
    void syncRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/moderation/reading-sources/sync"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void syncReturnsForbiddenForNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/moderation/reading-sources/sync")
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void syncReturnsTheReportMappedToDtoForAdminUser() throws Exception {
        ComicReadingSource created = source(SOURCE_ID, "MangaDex", ComicReadingSourceStatus.APPROVED);
        ComicReadingSourceDescriptor skipped = new ComicReadingSourceDescriptor("9", "Broken", "");
        when(comicReadingSourceSyncService.sync(USER_ID)).thenReturn(new ComicReadingSourceSyncReport(
                List.of(new SyncedSource("suwayomi", created)), List.of(), List.of(),
                List.of(new SkippedSource("suwayomi", skipped, SkipReason.NO_URL)),
                List.of(new DiscoveryOutcome.Failed("olympus", new IllegalStateException("down")))));

        mockMvc.perform(post("/api/moderation/reading-sources/sync")
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created[0].providerKey").value("suwayomi"))
                .andExpect(jsonPath("$.created[0].source.name").value("MangaDex"))
                .andExpect(jsonPath("$.linked").isEmpty())
                .andExpect(jsonPath("$.updated").isEmpty())
                .andExpect(jsonPath("$.skipped[0].externalId").value("9"))
                .andExpect(jsonPath("$.skipped[0].reason").value("NO_URL"))
                .andExpect(jsonPath("$.failedProviders[0].providerKey").value("olympus"))
                .andExpect(jsonPath("$.failedProviders[0].message").value("down"));
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