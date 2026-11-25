package com.github.kio7po.comic_tracker.adapter.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.github.kio7po.comic_tracker.adapter.rest.exception.ProblemType;
import com.github.kio7po.comic_tracker.adapter.rest.security.CoverUrlSigner;
import com.github.kio7po.comic_tracker.adapter.rest.security.CoverUrlSigner.CoverAddress;
import com.github.kio7po.comic_tracker.adapter.rest.security.JwtDecoderConfig;
import com.github.kio7po.comic_tracker.adapter.rest.security.SecurityConfig;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingCoverUnavailableException;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverImage;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingCoverService;

// Imports the real SecurityConfig so the test reflects that the endpoint is reachable without a session: it is
// protected by the signature of the address, not by the user's token.
@WebMvcTest(ComicReadingCoverController.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class })
class ComicReadingCoverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ComicReadingCoverService comicReadingCoverService;
    @MockitoBean
    private CoverUrlSigner coverUrlSigner;

    @Test
    void findCoverReturnsTheImageWithItsContentTypeAndCacheHeaderWithoutASession() throws Exception {
        when(coverUrlSigner.verify("signed-token")).thenReturn(Optional.of(new CoverAddress("suwayomi", "731")));
        when(coverUrlSigner.minValidity()).thenReturn(Duration.ofHours(6));
        when(comicReadingCoverService.findCover("suwayomi", "731"))
                .thenReturn(Optional.of(new ComicReadingCoverImage(new byte[] { 1, 2, 3 }, "image/webp")));

        mockMvc.perform(get("/api/covers/{token}", "signed-token"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/webp"))
                .andExpect(header().string("Cache-Control", "max-age=21600, private"))
                .andExpect(content().bytes(new byte[] { 1, 2, 3 }));
    }

    @Test
    void findCoverReturnsForbiddenWithoutAskingAnyProviderWhenTheSignatureIsNotValid() throws Exception {
        when(coverUrlSigner.verify("forged-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/covers/{token}", "forged-token")).andExpect(status().isForbidden());

        verify(comicReadingCoverService, never()).findCover(any(), any());
    }

    @Test
    void findCoverReturnsNotFoundWhenThereIsNoSuchCover() throws Exception {
        when(coverUrlSigner.verify("signed-token")).thenReturn(Optional.of(new CoverAddress("suwayomi", "731")));
        when(comicReadingCoverService.findCover("suwayomi", "731")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/covers/{token}", "signed-token")).andExpect(status().isNotFound());
    }

    @Test
    void findCoverReturnsBadGatewayWhenTheProviderCannotServeIt() throws Exception {
        when(coverUrlSigner.verify("signed-token")).thenReturn(Optional.of(new CoverAddress("suwayomi", "731")));
        when(comicReadingCoverService.findCover("suwayomi", "731"))
                .thenThrow(new ComicReadingCoverUnavailableException("suwayomi", new RuntimeException("down")));

        mockMvc.perform(get("/api/covers/{token}", "signed-token"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.type").value(ProblemType.READING_COVER_UNAVAILABLE));
    }

}
