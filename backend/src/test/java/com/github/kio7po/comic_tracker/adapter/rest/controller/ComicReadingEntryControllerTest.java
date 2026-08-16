package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.kio7po.comic_tracker.adapter.rest.exception.ProblemType;
import com.github.kio7po.comic_tracker.adapter.rest.security.JwtDecoderConfig;
import com.github.kio7po.comic_tracker.adapter.rest.security.SecurityConfig;
import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntrySortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotApprovedException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingEntryException;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingEntryService;
import com.github.kio7po.comic_tracker.domain.service.ComicService;

// Imports the real SecurityConfig so this test reflects actual authorization behavior
// (GET public, submit authenticated, approve/reject ADMIN-only) instead of drifting from it
// silently if those rules ever change.
@WebMvcTest(ComicReadingEntryController.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class })
class ComicReadingEntryControllerTest {

    private static final String SLUG = "berserk";
    private static final Long COMIC_ID = 1L;
    private static final Long SOURCE_ID = 2L;
    private static final Long ENTRY_ID = 3L;
    // Matches the "1" subject set on every authenticated jwt() request below.
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComicReadingEntryService comicReadingEntryService;
    @MockitoBean
    private ComicService comicService;

    private static Comic comic() {
        Comic comic = new Comic();
        comic.setId(COMIC_ID);
        comic.setSlug(SLUG);
        comic.setTitle("Berserk");
        return comic;
    }

    private static ComicReadingSource source() {
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        source.setSlug("mangadex");
        source.setName("MangaDex");
        source.setUrl("https://mangadex.org");
        source.setStatus(ComicReadingSourceStatus.APPROVED);
        return source;
    }

    private static ComicReadingEntry entry() {
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setId(ENTRY_ID);
        entry.setUrl("https://mangadex.org/title/123");
        entry.setLocale("es-ES");
        entry.setStatus(ComicReadingEntryStatus.PENDING);
        entry.setSource(source());
        entry.setComic(comic());
        return entry;
    }

    @Test
    void findByComicReturnsEntriesWhenComicExists() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(comicReadingEntryService.findByComic(COMIC_ID, null)).thenReturn(List.of(entry()));

        mockMvc.perform(get("/api/comics/{slug}/reading-entries", SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ENTRY_ID))
                .andExpect(jsonPath("$[0].source.slug").value("mangadex"));
    }

    @Test
    void findByComicPassesStatusFilterThrough() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(comicReadingEntryService.findByComic(COMIC_ID, ComicReadingEntryStatus.APPROVED))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/comics/{slug}/reading-entries", SLUG).param("status", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void findByComicReturnsNotFoundWhenComicDoesNotExist() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/comics/{slug}/reading-entries", SLUG))
                .andExpect(status().isNotFound());
    }

    @Test
    void submitRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/comics/{slug}/reading-entries", SLUG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceId":2,"url":"https://mangadex.org/title/123","locale":"es-ES"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitWithExistingSourceReturnsCreated() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, "https://mangadex.org/title/123", "es-ES",
                USER_ID)).thenReturn(entry());

        mockMvc.perform(post("/api/comics/{slug}/reading-entries", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceId":2,"url":"https://mangadex.org/title/123","locale":"es-ES"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ENTRY_ID));
    }

    @Test
    void submitWithNewSourceReturnsCreated() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(comicReadingEntryService.submitWithNewSource(COMIC_ID, "MangaDex", "https://mangadex.org",
                "https://mangadex.org/title/123", "es-ES", USER_ID)).thenReturn(entry());

        mockMvc.perform(post("/api/comics/{slug}/reading-entries", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceName":"MangaDex","sourceUrl":"https://mangadex.org",
                         "url":"https://mangadex.org/title/123","locale":"es-ES"}
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void submitReturnsBadRequestWhenBothSourceIdAndNewSourceDetailsAreGiven() throws Exception {
        mockMvc.perform(post("/api/comics/{slug}/reading-entries", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceId":2,"sourceName":"MangaDex","sourceUrl":"https://mangadex.org",
                         "url":"https://mangadex.org/title/123","locale":"es-ES"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitReturnsBadRequestWhenNeitherSourceIdNorNewSourceDetailsAreGiven() throws Exception {
        mockMvc.perform(post("/api/comics/{slug}/reading-entries", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"url":"https://mangadex.org/title/123","locale":"es-ES"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitReturnsBadRequestWhenUrlIsMalformed() throws Exception {
        mockMvc.perform(post("/api/comics/{slug}/reading-entries", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceId":2,"url":"not-a-url","locale":"es-ES"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitReturnsBadRequestWhenLocaleIsNotValidIso() throws Exception {
        mockMvc.perform(post("/api/comics/{slug}/reading-entries", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceId":2,"url":"https://mangadex.org/title/123","locale":"xx"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitReturnsConflictWhenEntryAlreadyExists() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(comicReadingEntryService.submit(any(), any(), any(), any(), any()))
                .thenThrow(new DuplicateComicReadingEntryException(COMIC_ID, SOURCE_ID,
                        "https://mangadex.org/title/123"));

        mockMvc.perform(post("/api/comics/{slug}/reading-entries", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceId":2,"url":"https://mangadex.org/title/123","locale":"es-ES"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.DUPLICATE_READING_ENTRY));
    }

    @Test
    void submitReturnsNotFoundWhenComicDoesNotExist() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/comics/{slug}/reading-entries", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"sourceId":2,"url":"https://mangadex.org/title/123","locale":"es-ES"}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void approveRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/reading-entries/{id}/approve", ENTRY_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveReturnsForbiddenForNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/reading-entries/{id}/approve", ENTRY_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveReturnsOkForAdminUser() throws Exception {
        ComicReadingEntry approved = entry();
        approved.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicReadingEntryService.approve(ENTRY_ID, USER_ID)).thenReturn(approved);

        mockMvc.perform(post("/api/reading-entries/{id}/approve", ENTRY_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approveReturnsConflictWhenSourceIsNotApproved() throws Exception {
        when(comicReadingEntryService.approve(ENTRY_ID, USER_ID))
                .thenThrow(new ComicReadingSourceNotApprovedException(SOURCE_ID, ComicReadingSourceStatus.PENDING));

        mockMvc.perform(post("/api/reading-entries/{id}/approve", ENTRY_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_SOURCE_NOT_APPROVED));
    }

    @Test
    void rejectRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/reading-entries/{id}/reject", ENTRY_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectReturnsForbiddenForNonAdminUser() throws Exception {
        mockMvc.perform(post("/api/reading-entries/{id}/reject", ENTRY_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectReturnsOkForAdminUser() throws Exception {
        ComicReadingEntry rejected = entry();
        rejected.setStatus(ComicReadingEntryStatus.REJECTED);
        when(comicReadingEntryService.reject(ENTRY_ID, USER_ID)).thenReturn(rejected);

        mockMvc.perform(post("/api/reading-entries/{id}/reject", ENTRY_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void rejectReturnsConflictWhenEntryAlreadyReviewed() throws Exception {
        when(comicReadingEntryService.reject(ENTRY_ID, USER_ID))
                .thenThrow(new ComicReadingEntryAlreadyReviewedException(ENTRY_ID, ComicReadingEntryStatus.APPROVED));

        mockMvc.perform(post("/api/reading-entries/{id}/reject", ENTRY_ID)
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_ENTRY_ALREADY_REVIEWED));
    }

    @Test
    void findByStatusInRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/reading-entries").param("statuses", "PENDING"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findByStatusInReturnsForbiddenForNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/reading-entries")
                .param("statuses", "PENDING")
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByStatusInReturnsEntriesWithComicForAdminUser() throws Exception {
        when(comicReadingEntryService.findByStatusIn(List.of(ComicReadingEntryStatus.PENDING),
                ComicReadingEntrySortField.CREATED_AT, SortDirection.ASC, 20, 0))
                .thenReturn(new Page<>(List.of(entry()), false, 1));

        mockMvc.perform(get("/api/reading-entries")
                .param("statuses", "PENDING")
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].entry.id").value(ENTRY_ID))
                .andExpect(jsonPath("$.items[0].comic.slug").value(SLUG))
                .andExpect(jsonPath("$.items[0].comic.title").value("Berserk"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void findByStatusInPassesSortAndPaginationParamsThrough() throws Exception {
        when(comicReadingEntryService.findByStatusIn(List.of(ComicReadingEntryStatus.PENDING),
                ComicReadingEntrySortField.CREATED_AT, SortDirection.DESC, 10, 5))
                .thenReturn(new Page<>(List.of(), false, 0));

        mockMvc.perform(get("/api/reading-entries")
                .param("statuses", "PENDING")
                .param("direction", "DESC")
                .param("limit", "10")
                .param("offset", "5")
                .with(jwt().jwt(builder -> builder.subject("1"))
                        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void findByComicPropagatesComicNotFoundAsNotFound() throws Exception {
        // Defensive: ComicNotFoundException would only surface here if comicService/comicReadingEntryService
        // disagreed about the comic's existence between the controller's lookup and the service call.
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(comicReadingEntryService.findByComic(COMIC_ID, null)).thenThrow(new ComicNotFoundException(COMIC_ID));

        mockMvc.perform(get("/api/comics/{slug}/reading-entries", SLUG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.COMIC_NOT_FOUND));
    }

}
