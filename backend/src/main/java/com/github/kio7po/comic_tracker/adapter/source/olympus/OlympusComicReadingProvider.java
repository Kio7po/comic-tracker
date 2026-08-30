package com.github.kio7po.comic_tracker.adapter.source.olympus;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.github.kio7po.comic_tracker.domain.common.TextSearch;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingProvider;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDetails;

@Component
public class OlympusComicReadingProvider implements ComicReadingProvider {

    private static final String SERIES_PATH_PREFIX = "/series/comic-";
    private static final String TYPE_COMIC = "comic";

    private final RestClient restClient;
    private final String baseUrl;
    private final String panelBaseUrl;
    private final String host;

    public OlympusComicReadingProvider(RestClient.Builder restClientBuilder,
            @Value("${olympus.base-url:https://olympusxyz.com}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
        this.panelBaseUrl = baseUrl.replace("https://", "https://panel.");
        this.host = URI.create(baseUrl).getHost().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean supports(String url) {
        try {
            String urlHost = URI.create(url).getHost();
            return urlHost != null && urlHost.equalsIgnoreCase(host);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Optional<ComicReadingSourceDetails> fetch(String url) {
        Optional<String> slug = extractSlug(url);
        if (slug.isEmpty()) {
            return Optional.empty();
        }
        OlympusMangaDto manga = fetchManga(slug.get());
        if (manga == null) {
            return Optional.empty();
        }
        OlympusChapterListResponseDto chapters = fetchChapters(slug.get());
        return Optional.of(OlympusComicReadingMapper.toDetails(manga, chapters));
    }

    @Override
    public List<ComicReadingSearchResult> search(String keywords) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl).path("/api/series/list").build().toUri();
        OlympusMangaListResponseDto response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(OlympusMangaListResponseDto.class);
        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream()
                .filter(dto -> matchesKeywords(dto, keywords))
                .map(dto -> OlympusComicReadingMapper.toSearchResult(dto, baseUrl))
                .toList();
    }

    private static boolean matchesKeywords(OlympusMangaDto dto, String keywords) {
        return TYPE_COMIC.equals(dto.type()) && TextSearch.matches(keywords, dto.name());
    }

    private OlympusMangaDto fetchManga(String slug) {
        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/api/series/{slug}")
                .queryParam("type", TYPE_COMIC)
                .buildAndExpand(slug)
                .toUri();
        try {
            OlympusMangaDetailResponseDto response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OlympusMangaDetailResponseDto.class);
            return response == null ? null : response.data();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    private OlympusChapterListResponseDto fetchChapters(String slug) {
        URI uri = UriComponentsBuilder.fromUriString(panelBaseUrl)
                .path("/api/series/{slug}/chapters")
                .queryParam("page", 1)
                .queryParam("direction", "desc")
                .queryParam("type", TYPE_COMIC)
                .buildAndExpand(slug)
                .toUri();
        return restClient.get()
                .uri(uri)
                .retrieve()
                .body(OlympusChapterListResponseDto.class);
    }

    private static Optional<String> extractSlug(String url) {
        String path;
        try {
            path = URI.create(url).getPath();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (path == null) {
            return Optional.empty();
        }
        int index = path.indexOf(SERIES_PATH_PREFIX);
        if (index == -1) {
            return Optional.empty();
        }
        String slug = path.substring(index + SERIES_PATH_PREFIX.length());
        int nextSlash = slug.indexOf('/');
        if (nextSlash != -1) {
            slug = slug.substring(0, nextSlash);
        }
        return slug.isBlank() ? Optional.empty() : Optional.of(slug);
    }
}
