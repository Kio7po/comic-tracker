package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Plain transport to Suwayomi's GraphQL endpoint: no caching and no rate limiting, those belong to the provider.
 * Every id it takes or returns is Suwayomi's own, not ours.
 */
@Component
class SuwayomiGraphQlClient {

    private static final String SOURCES_QUERY = "{ sources { nodes { id name lang homeUrl } } }";
    private static final String STORED_MANGA_QUERY = """
            query($sourceId: LongString!, $realUrl: String!) {
              mangas(filter: { sourceId: { equalTo: $sourceId }, realUrl: { equalTo: $realUrl } }) {
                nodes { id title realUrl }
              }
            }""";
    private static final String SOURCE_SEARCH_MUTATION = """
            mutation($source: LongString!, $query: String!) {
              fetchSourceManga(input: { source: $source, type: SEARCH, query: $query, page: 1 }) {
                hasNextPage
                mangas { id title realUrl }
              }
            }""";
    private static final String MANGA_AND_CHAPTERS_MUTATION = """
            mutation($id: Int!) {
              fetchMangaAndChapters(input: { id: $id, fetchManga: true, fetchChapters: true }) {
                manga { id title realUrl }
                chapters { uploadDate }
              }
            }""";

    private final RestClient restClient;
    private final URI graphQlUri;

    SuwayomiGraphQlClient(RestClient.Builder restClientBuilder,
            @Value("${suwayomi.base-url:http://localhost:4567}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.graphQlUri = UriComponentsBuilder.fromUriString(baseUrl).path("/api/graphql").build().toUri();
    }

    List<SuwayomiSourceDto> fetchSources() {
        SourcesData data = executeGraphQl(SOURCES_QUERY, Map.of(), new ParameterizedTypeReference<>() {
        });
        return data.sources().nodes();
    }

    /**
     * Looks only among the works Suwayomi already has stored: it can't resolve an address it hasn't seen.
     */
    Optional<SuwayomiMangaDto> findStoredManga(String suwayomiSourceId, String realUrl) {
        StoredMangasData data = executeGraphQl(STORED_MANGA_QUERY,
                Map.of("sourceId", suwayomiSourceId, "realUrl", realUrl), new ParameterizedTypeReference<>() {
                });
        return data.mangas().nodes().stream().findFirst();
    }

    /**
     * Asks the site, so it's a call to the remote site, and Suwayomi stores every result it gets back.
     *
     * @return the first page only.
     */
    SuwayomiSearchPageDto searchSourceMangas(String suwayomiSourceId, String keywords) {
        SourceSearchData data = executeGraphQl(SOURCE_SEARCH_MUTATION,
                Map.of("source", suwayomiSourceId, "query", keywords), new ParameterizedTypeReference<>() {
                });
        return data.fetchSourceManga();
    }

    /**
     * Asks the site, so it's a call to the remote site.
     */
    SuwayomiMangaWithChaptersDto fetchMangaAndChapters(int suwayomiMangaId) {
        MangaAndChaptersData data = executeGraphQl(MANGA_AND_CHAPTERS_MUTATION, Map.of("id", suwayomiMangaId),
                new ParameterizedTypeReference<>() {
                });
        return data.fetchMangaAndChapters();
    }

    private <T> T executeGraphQl(String query, Map<String, Object> variables,
            ParameterizedTypeReference<SuwayomiGraphQlResponseDto<T>> responseType) {
        SuwayomiGraphQlResponseDto<T> response = restClient.post()
                .uri(graphQlUri)
                .body(Map.of("query", query, "variables", variables))
                .retrieve()
                .body(responseType);
        if (response == null) {
            throw new SuwayomiGraphQlException("Suwayomi returned an empty response");
        }
        if (response.errors() != null && !response.errors().isEmpty()) {
            throw new SuwayomiGraphQlException(response.errors().get(0).message());
        }
        if (response.data() == null) {
            throw new SuwayomiGraphQlException("Suwayomi returned no data");
        }
        return response.data();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Nodes<T>(List<T> nodes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SourcesData(Nodes<SuwayomiSourceDto> sources) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StoredMangasData(Nodes<SuwayomiMangaDto> mangas) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SourceSearchData(SuwayomiSearchPageDto fetchSourceManga) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MangaAndChaptersData(SuwayomiMangaWithChaptersDto fetchMangaAndChapters) {
    }
}
