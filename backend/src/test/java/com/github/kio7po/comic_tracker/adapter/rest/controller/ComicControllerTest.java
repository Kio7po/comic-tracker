package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.kio7po.comic_tracker.adapter.rest.security.JwtDecoderConfig;
import com.github.kio7po.comic_tracker.adapter.rest.security.SecurityConfig;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.service.ComicService;

// Imports the real SecurityConfig so this test reflects actual authorization behavior
// (GET /api/comics public) instead of drifting from it silently if those rules ever change.
@WebMvcTest(ComicController.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class })
class ComicControllerTest {

    private static final String SLUG = "berserk";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComicService comicService;

    private static Comic comic() {
        Comic comic = new Comic();
        comic.setId(1L);
        comic.setSlug(SLUG);
        comic.setTitle("Berserk");
        comic.setMediaType(ComicMediaType.MANGA);
        comic.setStatus(ComicStatus.ONGOING);
        return comic;
    }

    @Test
    void getBySlugReturnsComicWhenFound() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic()));

        mockMvc.perform(get("/api/comics/{slug}", SLUG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(SLUG))
                .andExpect(jsonPath("$.title").value("Berserk"));
    }

    @Test
    void getBySlugReturnsNotFoundWhenMissing() throws Exception {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/comics/{slug}", SLUG))
                .andExpect(status().isNotFound());
    }

}
