package com.github.kio7po.comic_tracker.adapter.source.olympus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDetails;

class OlympusComicReadingProviderTest {

    private static final String BASE_URL = "https://olympusxyz.com";
    private static final String PANEL_BASE_URL = "https://panel.olympusxyz.com";

    private MockRestServiceServer server;
    private OlympusComicReadingProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OlympusComicReadingProvider(builder, BASE_URL);
    }

    private static String mangaDetailJson(String name) {
        return """
                {
                  "data": { "name": "%s", "slug": "berserk", "type": "comic" }
                }
                """.formatted(name);
    }

    private static String chaptersJson(int total, String... publishedAt) {
        String data = java.util.Arrays.stream(publishedAt)
                .map(date -> "{ \"published_at\": \"%s\" }".formatted(date))
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return """
                {
                  "data": [%s],
                  "meta": { "total": %d }
                }
                """.formatted(data, total);
    }

    private static String seriesListJson() {
        return """
                {
                  "data": [
                    { "name": "The Sword King", "slug": "sword-king", "type": "comic" },
                    { "name": "The Sword King Light Novel", "slug": "sword-king-novel", "type": "novel" },
                    { "name": "One Piece", "slug": "one-piece", "type": "comic" }
                  ]
                }
                """;
    }

    @Test
    void supports_trueForAUrlOnTheConfiguredHost() {
        assertThat(provider.supports("https://olympusxyz.com/series/comic-berserk")).isTrue();
    }

    @Test
    void supports_isCaseInsensitiveOnTheHost() {
        assertThat(provider.supports("https://OlympusXYZ.com/series/comic-berserk")).isTrue();
    }

    @Test
    void supports_falseForADifferentHost() {
        assertThat(provider.supports("https://example.com/series/comic-berserk")).isFalse();
    }

    @Test
    void supports_falseForAMalformedUrl() {
        assertThat(provider.supports("not a url")).isFalse();
    }

    @Test
    void fetch_requestsTheMangaDetailAndChaptersEndpointsForTheUrlsSlug() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(chaptersJson(374, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        provider.fetch("https://olympusxyz.com/series/comic-berserk");

        server.verify();
    }

    @Test
    void fetch_mapsASuccessfulResponseToDetails() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(374, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        Optional<ComicReadingSourceDetails> result = provider.fetch("https://olympusxyz.com/series/comic-berserk");

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("Berserk");
        assertThat(result.get().availableChapters()).isEqualTo(374);
        assertThat(result.get().latestChapterAt()).isEqualTo(Instant.parse("2024-06-01T10:00:00.000000Z"));
    }

    @Test
    void fetch_extractsTheSlugFromAUrlWithExtraPathSegments() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(1, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        provider.fetch("https://olympusxyz.com/series/comic-berserk/some-extra-segment");

        server.verify();
    }

    @Test
    void fetch_returnsEmptyWithoutCallingTheApiWhenTheUrlHasNoRecognizableSlug() {
        // No server.expect(...) set up on purpose: the request must never reach the network -
        // MockRestServiceServer fails the test itself if it does.
        Optional<ComicReadingSourceDetails> result = provider.fetch("https://olympusxyz.com/other/path");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void fetch_returnsEmptyWhenTheMangaDetailEndpointRespondsNotFound() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withResourceNotFound());

        Optional<ComicReadingSourceDetails> result = provider.fetch("https://olympusxyz.com/series/comic-berserk");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void fetch_propagatesTheExceptionWhenTheMangaDetailEndpointRespondsServerError() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.fetch("https://olympusxyz.com/series/comic-berserk"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void fetch_handlesAnEmptyChapterListByLeavingLatestChapterAtNull() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(0), MediaType.APPLICATION_JSON));

        Optional<ComicReadingSourceDetails> result = provider.fetch("https://olympusxyz.com/series/comic-berserk");

        assertThat(result).isPresent();
        assertThat(result.get().availableChapters()).isZero();
        assertThat(result.get().latestChapterAt()).isNull();
    }

    @Test
    void search_requestsTheSeriesListEndpoint() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        provider.search("berserk");

        server.verify();
    }

    @Test
    void search_filtersByTypeComicAndCaseInsensitiveNameMatch() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        List<ComicReadingSearchResult> results = provider.search("SWORD");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("The Sword King");
        assertThat(results.get(0).url()).isEqualTo(BASE_URL + "/series/comic-sword-king");
    }

    @Test
    void search_matchesEveryWordOfTheQueryRegardlessOfOrder() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        // Reversed compared to the actual title ("The Sword King"), and skipping "the" entirely -
        // every word still has to match, but not in order or exhaustively.
        List<ComicReadingSearchResult> results = provider.search("king sword");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("The Sword King");
    }

    @Test
    void search_excludesResultsMissingAnyQueryWord() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        List<ComicReadingSearchResult> results = provider.search("sword lord");

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsAnEmptyListWhenDataIsNull() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess("{\"data\":null}", MediaType.APPLICATION_JSON));

        List<ComicReadingSearchResult> results = provider.search("berserk");

        assertThat(results).isEmpty();
    }

    @Test
    void search_propagatesTheExceptionWhenTheApiRespondsServerError() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.search("berserk"))
                .isInstanceOf(HttpServerErrorException.class);
    }
}
