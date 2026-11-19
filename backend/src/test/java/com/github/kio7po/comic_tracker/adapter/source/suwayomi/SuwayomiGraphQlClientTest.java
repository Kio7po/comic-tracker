package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class SuwayomiGraphQlClientTest {

    private static final String BASE_URL = "http://localhost:4567";
    private static final String GRAPHQL_URL = BASE_URL + "/api/graphql";

    private MockRestServiceServer server;
    private SuwayomiGraphQlClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SuwayomiGraphQlClient(builder, BASE_URL);
    }

    @Test
    void fetchSources_returnsTheSourcesOfTheInstalledExtensions() {
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        { "data": { "sources": { "nodes": [
                          { "id": "0", "name": "Local source", "lang": "localsourcelang", "homeUrl": null },
                          { "id": "5621901596522308538", "name": "LeerCapitulo", "lang": "es",
                            "homeUrl": "https://www.leercapitulo.co", "iconUrl": "/ignored" }
                        ] } } }
                        """, MediaType.APPLICATION_JSON));

        List<SuwayomiSourceDto> sources = client.fetchSources();

        assertThat(sources).containsExactly(
                new SuwayomiSourceDto("0", "Local source", "localsourcelang", null),
                new SuwayomiSourceDto("5621901596522308538", "LeerCapitulo", "es", "https://www.leercapitulo.co"));
        server.verify();
    }

    @Test
    void searchSourceMangas_sendsTheSourceAndKeywordsAsVariablesAndReturnsTheFirstPage() {
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.variables.source").value("5621901596522308538"))
                .andExpect(jsonPath("$.variables.query").value("solo leveling"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("page: 1")))
                .andRespond(withSuccess("""
                        { "data": { "fetchSourceManga": { "hasNextPage": true, "mangas": [
                          { "id": 1, "title": "Level Up Alone", "realUrl": null },
                          { "id": 2, "title": "Solo Leveling", "realUrl": "https://www.leercapitulo.co/manga/x/" }
                        ] } } }
                        """, MediaType.APPLICATION_JSON));

        SuwayomiSearchPageDto page = client.searchSourceMangas("5621901596522308538", "solo leveling");

        assertThat(page.hasNextPage()).isTrue();
        assertThat(page.mangas()).containsExactly(new SuwayomiMangaDto(1, "Level Up Alone", null),
                new SuwayomiMangaDto(2, "Solo Leveling", "https://www.leercapitulo.co/manga/x/"));
        server.verify();
    }

    @Test
    void findStoredManga_returnsTheStoredWorkWithThatAddress() {
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(jsonPath("$.variables.sourceId").value("5621901596522308538"))
                .andExpect(jsonPath("$.variables.realUrl").value("https://www.leercapitulo.co/manga/x/"))
                .andRespond(withSuccess("""
                        { "data": { "mangas": { "nodes": [
                          { "id": 2, "title": "Solo Leveling", "realUrl": "https://www.leercapitulo.co/manga/x/" }
                        ] } } }
                        """, MediaType.APPLICATION_JSON));

        Optional<SuwayomiMangaDto> manga = client.findStoredManga("5621901596522308538",
                "https://www.leercapitulo.co/manga/x/");

        assertThat(manga).contains(new SuwayomiMangaDto(2, "Solo Leveling", "https://www.leercapitulo.co/manga/x/"));
    }

    @Test
    void findStoredManga_returnsEmptyWhenSuwayomiHasNoStoredWorkWithThatAddress() {
        server.expect(requestTo(GRAPHQL_URL))
                .andRespond(withSuccess("{ \"data\": { \"mangas\": { \"nodes\": [] } } }",
                        MediaType.APPLICATION_JSON));

        assertThat(client.findStoredManga("5621901596522308538", "https://www.leercapitulo.co/manga/x/")).isEmpty();
    }

    @Test
    void fetchMangaAndChapters_sendsTheMangaIdAndReturnsTheWorkWithItsChapters() {
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(jsonPath("$.variables.id").value(2))
                .andRespond(withSuccess("""
                        { "data": { "fetchMangaAndChapters": {
                          "manga": { "id": 2, "title": "Solo Leveling", "realUrl": "https://www.leercapitulo.co/manga/x/" },
                          "chapters": [ { "uploadDate": "1700000000000" }, { "uploadDate": "0" } ]
                        } } }
                        """, MediaType.APPLICATION_JSON));

        SuwayomiMangaWithChaptersDto result = client.fetchMangaAndChapters(2);

        assertThat(result.manga()).isEqualTo(
                new SuwayomiMangaDto(2, "Solo Leveling", "https://www.leercapitulo.co/manga/x/"));
        assertThat(result.chapters()).containsExactly(new SuwayomiChapterDto("1700000000000"),
                new SuwayomiChapterDto("0"));
    }

    @Test
    void anyOperation_throwsWithTheGraphQlMessageWhenTheResponseCarriesErrors() {
        server.expect(requestTo(GRAPHQL_URL))
                .andRespond(withSuccess("""
                        { "data": null, "errors": [ { "message": "Source not found" } ] }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchSourceMangas("1", "solo"))
                .isInstanceOf(SuwayomiGraphQlException.class)
                .hasMessage("Source not found");
    }

    @Test
    void anyOperation_throwsWhenTheResponseHasNeitherDataNorErrors() {
        server.expect(requestTo(GRAPHQL_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::fetchSources).isInstanceOf(SuwayomiGraphQlException.class);
    }

    @Test
    void anyOperation_propagatesHttpFailures() {
        server.expect(requestTo(GRAPHQL_URL)).andRespond(withServerError());

        assertThatThrownBy(client::fetchSources).isInstanceOf(HttpServerErrorException.class);
    }
}
