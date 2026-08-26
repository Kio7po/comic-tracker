package com.github.kio7po.comic_tracker.adapter.metadata.tenrai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;

class TenraiComicMetadataProviderTest {

    private static final String BASE_URL = "https://api.tenrai.org/v1";

    private MockRestServiceServer server;
    private TenraiComicMetadataProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new TenraiComicMetadataProvider(builder, BASE_URL);
    }

    private static String mangaJson(long malId, String title) {
        return """
                {
                  "data": {
                    "mal_id": %d,
                    "title": "%s"
                  }
                }
                """.formatted(malId, title);
    }

    private static String searchJson(boolean hasNextPage, int total) {
        return """
                {
                  "data": [
                    { "mal_id": 2, "title": "Berserk" }
                  ],
                  "pagination": {
                    "has_next_page": %s,
                    "items": {
                      "total": %d
                    }
                  }
                }
                """.formatted(hasNextPage, total);
    }

    @Test
    void fetch_requestsTheMangaEndpointByExternalId() {
        server.expect(requestTo(BASE_URL + "/manga/2"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mangaJson(2, "Berserk"), MediaType.APPLICATION_JSON));

        provider.fetch("2");

        server.verify();
    }

    @Test
    void fetch_mapsASuccessfulResponseToAComicMetadataResult() {
        server.expect(requestTo(BASE_URL + "/manga/2"))
                .andRespond(withSuccess(mangaJson(2, "Berserk"), MediaType.APPLICATION_JSON));

        var result = provider.fetch("2");

        assertThat(result).isPresent();
        assertThat(result.get().getExternalId()).isEqualTo("2");
        assertThat(result.get().getComic().getTitle()).isEqualTo("Berserk");
    }

    @Test
    void fetch_returnsEmptyWhenTheApiRespondsNotFound() {
        server.expect(requestTo(BASE_URL + "/manga/999999"))
                .andRespond(withResourceNotFound());

        var result = provider.fetch("999999");

        assertThat(result).isEmpty();
    }

    @Test
    void fetch_propagatesTheExceptionWhenTheApiRespondsServerError() {
        server.expect(requestTo(BASE_URL + "/manga/2"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.fetch("2"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void fetch_propagatesTheExceptionOnATimeout() {
        server.expect(requestTo(BASE_URL + "/manga/2"))
                .andRespond(request -> {
                    throw new SocketTimeoutException("timeout");
                });

        assertThatThrownBy(() -> provider.fetch("2"))
                .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    void search_buildsTheRequestWithQueryAndPagination() {
        server.expect(requestTo(startsWith(BASE_URL + "/manga")))
                .andExpect(queryParam("q", "berserk"))
                .andExpect(queryParam("page", "2"))
                .andExpect(queryParam("limit", "10"))
                .andRespond(withSuccess(searchJson(false, 1), MediaType.APPLICATION_JSON));

        provider.search("berserk", 10, 10, null, null, null);

        server.verify();
    }

    @Test
    void search_appliesSfwStrictWhenNsfwCeilingIsNone() {
        server.expect(requestTo(startsWith(BASE_URL + "/manga")))
                .andExpect(queryParam("sfw-strict", "true"))
                .andRespond(withSuccess(searchJson(false, 1), MediaType.APPLICATION_JSON));

        provider.search("berserk", 10, 0, NsfwRating.NONE, null, null);

        server.verify();
    }

    @Test
    void search_appliesSfwWhenNsfwCeilingIsSuggestive() {
        server.expect(requestTo(startsWith(BASE_URL + "/manga")))
                .andExpect(queryParam("sfw", "true"))
                .andRespond(withSuccess(searchJson(false, 1), MediaType.APPLICATION_JSON));

        provider.search("berserk", 10, 0, NsfwRating.SUGGESTIVE, null, null);

        server.verify();
    }

    @Test
    void search_appliesNoFilterWhenNsfwCeilingIsExplicit() {
        server.expect(requestTo(allOf(
                        startsWith(BASE_URL + "/manga"),
                        not(containsString("sfw")))))
                .andRespond(withSuccess(searchJson(false, 1), MediaType.APPLICATION_JSON));

        provider.search("berserk", 10, 0, NsfwRating.EXPLICIT, null, null);

        server.verify();
    }

    @Test
    void search_omitsStatusAndTypeWhenTheyHaveNoTenraiEquivalent() {
        server.expect(requestTo(allOf(
                        startsWith(BASE_URL + "/manga"),
                        not(containsString("status")),
                        not(containsString("type")))))
                .andRespond(withSuccess(searchJson(false, 1), MediaType.APPLICATION_JSON));

        provider.search("berserk", 10, 0, null, ComicStatus.OTHER, ComicMediaType.WEBTOON);

        server.verify();
    }

    @Test
    void search_mapsResultsHasNextPageAndTotalFromPagination() {
        server.expect(requestTo(startsWith(BASE_URL + "/manga")))
                .andRespond(withSuccess(searchJson(true, 42), MediaType.APPLICATION_JSON));

        Page<ComicMetadataResult> page = provider.search("berserk", 10, 0, null, null, null);

        assertThat(page.isExistMoreItems()).isTrue();
        assertThat(page.getTotalItems()).isEqualTo(42);
        assertThat(page.getItems()).hasSize(1);
        assertThat(page.getItems().get(0).getExternalId()).isEqualTo("2");
    }

    @Test
    void search_cappsExistMoreItemsAtTheMaxReachablePage() {
        // page 1000 (offset 9990, limit 10) is Tenrai/MyAnimeList's last reachable page;
        // hasNextPage=true here should still be overridden since page 1001 isn't fetchable.
        server.expect(requestTo(startsWith(BASE_URL + "/manga")))
                .andRespond(withSuccess(searchJson(true, 100_000), MediaType.APPLICATION_JSON));

        Page<ComicMetadataResult> page = provider.search("berserk", 10, 9990, null, null, null);

        assertThat(page.isExistMoreItems()).isFalse();
    }

    @Test
    void search_capsTotalItemsAtTheMaxReachablePage() {
        server.expect(requestTo(startsWith(BASE_URL + "/manga")))
                .andRespond(withSuccess(searchJson(true, 100_000), MediaType.APPLICATION_JSON));

        Page<ComicMetadataResult> page = provider.search("berserk", 10, 0, null, null, null);

        assertThat(page.getTotalItems()).isEqualTo(10_000);
    }

    @Test
    void search_returnsAnEmptyPageWhenDataIsNull() {
        server.expect(requestTo(startsWith(BASE_URL + "/manga")))
                .andRespond(withSuccess("{\"data\":null,\"pagination\":null}", MediaType.APPLICATION_JSON));

        Page<ComicMetadataResult> page = provider.search("berserk", 10, 0, null, null, null);

        assertThat(page.getItems()).isEmpty();
        assertThat(page.isExistMoreItems()).isFalse();
        assertThat(page.getTotalItems()).isNull();
    }

    @Test
    void search_propagatesTheExceptionWhenTheApiRespondsServerError() {
        server.expect(requestTo(startsWith(BASE_URL + "/manga")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.search("berserk", 10, 0, null, null, null))
                .isInstanceOf(HttpServerErrorException.class);
    }
}
