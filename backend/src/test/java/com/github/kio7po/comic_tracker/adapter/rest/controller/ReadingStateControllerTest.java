package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

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
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ReadingState;
import com.github.kio7po.comic_tracker.domain.enums.ReadingStateStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateNotFoundException;
import com.github.kio7po.comic_tracker.domain.service.ComicService;
import com.github.kio7po.comic_tracker.domain.service.ReadingStateService;

// Imports the real SecurityConfig so this test reflects actual authorization behavior
// (every reading-state operation requires authentication) instead of drifting from it silently
// if those rules ever change.
@WebMvcTest(ReadingStateController.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class })
class ReadingStateControllerTest {

    private static final String SLUG = "berserk";
    private static final Long COMIC_ID = 1L;
    private static final Long READING_STATE_ID = 2L;
    // Matches the "1" subject set on every authenticated jwt() request below.
    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReadingStateService readingStateService;
    @MockitoBean
    private ComicService comicService;

    private static Comic comic() {
        Comic comic = new Comic();
        comic.setId(COMIC_ID);
        comic.setSlug(SLUG);
        comic.setTitle("Berserk");
        return comic;
    }

    private static ReadingState readingState() {
        ReadingState readingState = new ReadingState();
        readingState.setId(READING_STATE_ID);
        readingState.setStatus(ReadingStateStatus.READING);
        readingState.setChapters(12);
        readingState.setNotes("Great so far");
        readingState.setComic(comic());
        return readingState;
    }

    @Test
    void getByComicRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/comics/{slug}/reading-state", SLUG))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getByComicReturnsNotFoundWhenComicDoesNotExist() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByComicReturnsNotFoundWhenNotTracked() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(readingStateService.findByUserAndComic(USER_ID, COMIC_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByComicReturnsReadingStateWhenTracked() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(readingStateService.findByUserAndComic(USER_ID, COMIC_ID)).thenReturn(Optional.of(readingState()));

        mockMvc.perform(get("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(READING_STATE_ID))
                .andExpect(jsonPath("$.status").value("READING"))
                .andExpect(jsonPath("$.chapters").value(12));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/comics/{slug}/reading-state", SLUG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PLAN_TO_READ","chapters":0}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(readingStateService.create(USER_ID, COMIC_ID, ReadingStateStatus.PLAN_TO_READ, 0, null))
                .thenReturn(readingState());

        mockMvc.perform(post("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PLAN_TO_READ","chapters":0}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(READING_STATE_ID));
    }

    @Test
    void createForwardsNotesToService() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(readingStateService.create(USER_ID, COMIC_ID, ReadingStateStatus.PLAN_TO_READ, 0,
                "Recommended by a friend")).thenReturn(readingState());

        mockMvc.perform(post("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PLAN_TO_READ","chapters":0,"notes":"Recommended by a friend"}
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void createReturnsBadRequestWhenStatusIsMissing() throws Exception {
        mockMvc.perform(post("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"chapters":0}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturnsBadRequestWhenChaptersIsNegative() throws Exception {
        mockMvc.perform(post("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"READING","chapters":-1}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createReturnsNotFoundWhenComicDoesNotExist() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PLAN_TO_READ","chapters":0}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void createReturnsConflictWhenAlreadyTracked() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(readingStateService.create(USER_ID, COMIC_ID, ReadingStateStatus.PLAN_TO_READ, 0, null))
                .thenThrow(new ReadingStateAlreadyExistsException(USER_ID, COMIC_ID));

        mockMvc.perform(post("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"PLAN_TO_READ","chapters":0}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_STATE_ALREADY_EXISTS));
    }

    @Test
    void updateRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/comics/{slug}/reading-state", SLUG)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"READING","chapters":12}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateReturnsOk() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(readingStateService.update(USER_ID, COMIC_ID, ReadingStateStatus.READING, 12, null))
                .thenReturn(readingState());

        mockMvc.perform(put("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"READING","chapters":12}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chapters").value(12));
    }

    @Test
    void updateReturnsNotFoundWhenComicDoesNotExist() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"READING","chapters":12}
                        """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateReturnsNotFoundWhenNotTracked() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(readingStateService.update(USER_ID, COMIC_ID, ReadingStateStatus.READING, 12, null))
                .thenThrow(new ReadingStateNotFoundException(USER_ID, COMIC_ID));

        mockMvc.perform(put("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"status":"READING","chapters":12}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_STATE_NOT_FOUND));
    }

    @Test
    void deleteRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/comics/{slug}/reading-state", SLUG))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));

        mockMvc.perform(delete("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteReturnsNotFoundWhenComicDoesNotExist() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturnsNotFoundWhenNotTracked() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        doThrow(new ReadingStateNotFoundException(USER_ID, COMIC_ID)).when(readingStateService).delete(USER_ID,
                COMIC_ID);

        mockMvc.perform(delete("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_STATE_NOT_FOUND));
    }

    @Test
    void findByUserRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/reading-states"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void findByUserReturnsReadingStatesWithComic() throws Exception {
        when(readingStateService.findByUser(USER_ID)).thenReturn(List.of(readingState()));

        mockMvc.perform(get("/api/reading-states")
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].readingState.id").value(READING_STATE_ID))
                .andExpect(jsonPath("$[0].comic.slug").value(SLUG))
                .andExpect(jsonPath("$[0].comic.title").value("Berserk"));
    }

    @Test
    void propagatesComicNotFoundAsNotFound() throws Exception {
        // Defensive: ComicNotFoundException would only surface here if comicService/readingStateService
        // disagreed about the comic's existence between the controller's lookup and the service call.
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));
        when(readingStateService.findByUserAndComic(USER_ID, COMIC_ID)).thenThrow(new ComicNotFoundException(COMIC_ID));

        mockMvc.perform(get("/api/comics/{slug}/reading-state", SLUG)
                .with(jwt().jwt(builder -> builder.subject("1"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ProblemType.COMIC_NOT_FOUND));
    }

}
