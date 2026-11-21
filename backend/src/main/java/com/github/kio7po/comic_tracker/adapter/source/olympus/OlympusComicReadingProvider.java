package com.github.kio7po.comic_tracker.adapter.source.olympus;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import com.github.kio7po.comic_tracker.adapter.common.RateLimiterExecutor;
import com.github.kio7po.comic_tracker.domain.common.TextSearch;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryTarget;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingProvider;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceTarget;

// Provider written for one specific site: it has to come before the generic one (the last in the order).
@Component
@Order(1)
public class OlympusComicReadingProvider implements ComicReadingProvider {

    static final String KEY = "olympus";
    // Olympus es un único sitio, así que su identificador es fijo; así sigue siendo el mismo si cambia de dominio.
    static final String SOURCE_EXTERNAL_ID = "olympus";
    private static final String SOURCE_NAME = "Olympus Scanlation";
    private static final String SERIES_PATH_PREFIX = "/series/comic-";
    private static final String TYPE_COMIC = "comic";

    private final AtomicReference<SeriesListSnapshot> seriesList = new AtomicReference<>();
    private final RestClient restClient;
    private final String baseUrl;
    private final Duration seriesListTtl;
    private final String panelBaseUrl;
    private final String host;
    private final RateLimiterExecutor baseLimiter;
    private final RateLimiterExecutor panelLimiter;

    public OlympusComicReadingProvider(RestClient.Builder restClientBuilder,
            @Value("${olympus.base-url:https://olympusxyz.com}") String baseUrl,
            @Value("${olympus.series-list-ttl:10m}") Duration seriesListTtl,
            RateLimiterRegistry rateLimiterRegistry) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
        this.seriesListTtl = seriesListTtl;
        this.panelBaseUrl = baseUrl.replace("https://", "https://panel.");
        this.host = URI.create(baseUrl).getHost().toLowerCase(Locale.ROOT);
        this.baseLimiter = new RateLimiterExecutor(rateLimiterRegistry.rateLimiter("olympus-base"));
        this.panelLimiter = new RateLimiterExecutor(rateLimiterRegistry.rateLimiter("olympus-panel"));
    }

    @Override
    public String key() {
        return KEY;
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
    public List<ComicReadingSourceDescriptor> discoverSources() {
        return List.of(new ComicReadingSourceDescriptor(SOURCE_EXTERNAL_ID, SOURCE_NAME, baseUrl));
    }

    @Override
    public Optional<ComicReadingEntryDetails> fetch(ComicReadingSourceTarget source, ComicReadingEntryTarget entry) {
        Optional<String> slug = resolveSlug(entry.url(), entry.externalId());
        if (slug.isEmpty()) {
            return Optional.empty();
        }
        OlympusMangaDto manga = fetchManga(slug.get());
        if (manga == null) {
            return Optional.empty();
        }
        OlympusChapterListResponseDto chapters = fetchChapters(slug.get());
        return Optional.of(OlympusComicReadingMapper.toDetails(manga, chapters, baseUrl));
    }

    @Override
    public List<ComicReadingSearchResult> search(ComicReadingSourceTarget source, String keywords) {
        return listSeries().stream()
                .filter(dto -> matchesKeywords(dto, keywords))
                .map(dto -> OlympusComicReadingMapper.toSearchResult(dto, baseUrl))
                .toList();
    }

    // The slug is the only thing the detail endpoint accepts, but it changes periodically; the id
    // doesn't. The id can only be turned back into the current slug through the full series list.
    private Optional<String> resolveSlug(@Nullable String url, @Nullable String externalId) {
        if (externalId != null) {
            Optional<String> slug = listSeries().stream()
                    .filter(dto -> TYPE_COMIC.equals(dto.type()))
                    .filter(dto -> externalId.equals(OlympusComicReadingMapper.externalId(dto)))
                    .map(OlympusMangaDto::slug)
                    .findFirst();
            if (slug.isPresent()) {
                return slug;
            }
        }
        return url == null ? Optional.empty() : extractSlug(url);
    }

    // No synchronization: this runs on virtual threads (see ComicReadingEntryService), so at worst two
    // concurrent callers both fetch the list once and the later write wins, which is harmless.
    private List<OlympusMangaDto> listSeries() {
        Instant now = Instant.now();
        SeriesListSnapshot cached = seriesList.get();
        if (cached != null && now.isBefore(cached.fetchedAt().plus(seriesListTtl))) {
            return cached.series();
        }
        URI uri = UriComponentsBuilder.fromUriString(baseUrl).path("/api/series/list").build().toUri();
        OlympusMangaListResponseDto response = baseLimiter.execute(() -> restClient.get()
                .uri(uri)
                .retrieve()
                .body(OlympusMangaListResponseDto.class));
        List<OlympusMangaDto> series = response == null || response.data() == null ? List.of()
                : List.copyOf(response.data());
        seriesList.set(new SeriesListSnapshot(now, series));
        return series;
    }

    private record SeriesListSnapshot(Instant fetchedAt, List<OlympusMangaDto> series) {
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
            OlympusMangaDetailResponseDto response = baseLimiter.execute(() -> restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(OlympusMangaDetailResponseDto.class));
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
        return panelLimiter.execute(() -> restClient.get()
                .uri(uri)
                .retrieve()
                .body(OlympusChapterListResponseDto.class));
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
