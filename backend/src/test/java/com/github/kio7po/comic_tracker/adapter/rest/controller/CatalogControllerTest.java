package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.kio7po.comic_tracker.adapter.rest.security.JwtDecoderConfig;
import com.github.kio7po.comic_tracker.adapter.rest.security.SecurityConfig;
import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicMetadataSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.UnsupportedMetadataSourceException;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;
import com.github.kio7po.comic_tracker.domain.service.CatalogService;

// Imports the real SecurityConfig so this test reflects actual authorization behavior
// (catalog routes public) instead of drifting from it silently if those rules ever change.
@WebMvcTest(CatalogController.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class })
class CatalogControllerTest {

    private static final String SOURCE_SLUG = "myanimelist";
    private static final String EXTERNAL_ID = "152";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;

    private static Comic comic() {
        Comic comic = new Comic();
        comic.setId(1L);
        comic.setSlug("berserk");
        comic.setTitle("Berserk");
        comic.setMediaType(ComicMediaType.MANGA);
        comic.setStatus(ComicStatus.ONGOING);
        return comic;
    }

    @Test
    void searchReturnsMappedResults() throws Exception {
        ComicMetadataResult result = new ComicMetadataResult(SOURCE_SLUG, EXTERNAL_ID, comic());
        when(catalogService.search(eq("berserk"), eq(20), eq(0), isNull(), isNull(), isNull()))
                .thenReturn(new Page<>(List.of(result), false));

        mockMvc.perform(get("/api/catalog/search").param("keywords", "berserk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existMoreItems").value(false))
                .andExpect(jsonPath("$.items[0].sourceSlug").value(SOURCE_SLUG))
                .andExpect(jsonPath("$.items[0].externalId").value(EXTERNAL_ID))
                .andExpect(jsonPath("$.items[0].title").value("Berserk"));
    }

    @Test
    void importComicReturnsComicWhenFound() throws Exception {
        when(catalogService.importComic(SOURCE_SLUG, EXTERNAL_ID)).thenReturn(Optional.of(comic()));

        mockMvc.perform(post("/api/catalog/{sourceSlug}/{externalId}", SOURCE_SLUG, EXTERNAL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("berserk"))
                .andExpect(jsonPath("$.title").value("Berserk"));
    }

    @Test
    void importComicReturnsNotFoundWhenProviderCannotFetchTheComic() throws Exception {
        when(catalogService.importComic(SOURCE_SLUG, EXTERNAL_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/catalog/{sourceSlug}/{externalId}", SOURCE_SLUG, EXTERNAL_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void importComicReturnsBadRequestWhenSourceSlugIsNotSupported() throws Exception {
        when(catalogService.importComic(any(), any())).thenThrow(new UnsupportedMetadataSourceException("anilist"));

        mockMvc.perform(post("/api/catalog/{sourceSlug}/{externalId}", "anilist", EXTERNAL_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void importComicReturnsNotFoundWhenSourceIsNotSeeded() throws Exception {
        when(catalogService.importComic(any(), any()))
                .thenThrow(new ComicMetadataSourceNotFoundException(SOURCE_SLUG));

        mockMvc.perform(post("/api/catalog/{sourceSlug}/{externalId}", SOURCE_SLUG, EXTERNAL_ID))
                .andExpect(status().isNotFound());
    }

}
