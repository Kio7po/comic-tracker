package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.kio7po.comic_tracker.adapter.rest.security.JwtDecoderConfig;
import com.github.kio7po.comic_tracker.adapter.rest.security.SecurityConfig;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceService;

// Imports the real SecurityConfig so this test reflects actual authorization behavior
// (GET /api/reading-sources public) instead of drifting from it silently if those rules ever change.
@WebMvcTest(ComicReadingSourceController.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class })
class ComicReadingSourceControllerTest {

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
        return source;
    }

    @Test
    void findSelectableReturnsSourcesMappedToDto() throws Exception {
        when(comicReadingSourceService.findSelectable())
                .thenReturn(List.of(source(1L, "MangaDex", ComicReadingSourceStatus.PENDING)));

        mockMvc.perform(get("/api/reading-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].slug").value("mangadex"))
                .andExpect(jsonPath("$[0].name").value("MangaDex"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void findSelectableIsPublic() throws Exception {
        when(comicReadingSourceService.findSelectable()).thenReturn(List.of());

        mockMvc.perform(get("/api/reading-sources"))
                .andExpect(status().isOk());
    }

}