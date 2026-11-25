package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import com.github.kio7po.comic_tracker.adapter.common.RateLimiterExecutor;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverImage;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverProvider;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryTarget;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingProvider;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceTarget;

/**
 * Serves every site that has an extension installed in a Suwayomi server. It goes last in the provider order, so a
 * provider written for one specific site wins over it.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SuwayomiComicReadingProvider implements ComicReadingProvider, ComicReadingCoverProvider {

    static final String KEY = "suwayomi";
    static final String RATE_LIMITER_CONFIG_NAME = "suwayomi";
    private static final String RATE_LIMITER_NAME_PREFIX = "suwayomi-";

    private final AtomicReference<SuwayomiSourcesSnapshot> suwayomiSourcesCache = new AtomicReference<>();
    private final SuwayomiGraphQlClient client;
    private final Duration suwayomiSourcesTtl;
    private final RateLimiterRegistry rateLimiterRegistry;

    SuwayomiComicReadingProvider(SuwayomiGraphQlClient client,
            @Value("${suwayomi.sources-ttl:10m}") Duration suwayomiSourcesTtl,
            RateLimiterRegistry rateLimiterRegistry) {
        this.client = client;
        this.suwayomiSourcesTtl = suwayomiSourcesTtl;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public boolean supports(String url) {
        return findSuwayomiSourceByUrl(url).isPresent();
    }

    @Override
    public List<ComicReadingSourceDescriptor> discoverSources() {
        return listSuwayomiSources().stream().map(SuwayomiComicReadingMapper::toReadingSourceDescriptor).toList();
    }

    /**
     * Never returns empty: Suwayomi only knows the works it has already stored, so not finding one doesn't mean the
     * site doesn't have it. When it can't resolve the work it throws.
     */
    @Override
    public Optional<ComicReadingEntryDetails> fetch(ComicReadingSourceTarget source, ComicReadingEntryTarget entry) {
        String suwayomiSourceId = resolveSuwayomiSourceId(source);
        int suwayomiMangaId = resolveSuwayomiMangaId(suwayomiSourceId, entry);
        SuwayomiMangaWithChaptersDto suwayomiManga = rateLimiterOfSuwayomiSource(suwayomiSourceId)
                .execute(() -> client.fetchMangaAndChapters(suwayomiMangaId));
        if (suwayomiManga.manga().realUrl() == null) {
            throw new IllegalStateException("Suwayomi has no public URL for the work " + suwayomiMangaId);
        }
        return Optional.of(SuwayomiComicReadingMapper.toReadingEntryDetails(suwayomiManga));
    }

    /**
     * @return the first page of results only, up to what the site's extension returns per page.
     */
    @Override
    public List<ComicReadingSearchResult> search(ComicReadingSourceTarget source, String keywords) {
        String suwayomiSourceId = resolveSuwayomiSourceId(source);
        SuwayomiSearchPageDto firstPage = rateLimiterOfSuwayomiSource(suwayomiSourceId)
                .execute(() -> client.searchSourceMangas(suwayomiSourceId, keywords));
        return firstPage.mangas().stream().map(SuwayomiComicReadingMapper::toReadingSearchResult).toList();
    }

    /**
     * Suwayomi doesn't give the site's address of the image, only a path of its own, so the cover is served through
     * it. Its thumbnails are per work, so the cover id is the work's id, and since the path is built from it, it has
     * to be a number and nothing else.
     */
    @Override
    public Optional<ComicReadingCoverImage> fetchCover(String externalCoverId) {
        return client.fetchThumbnail(parseSuwayomiMangaId(externalCoverId));
    }

    private static int parseSuwayomiMangaId(String externalId) {
        try {
            return Integer.parseInt(externalId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a Suwayomi work id: " + externalId, e);
        }
    }

    private int resolveSuwayomiMangaId(String suwayomiSourceId, ComicReadingEntryTarget entry) {
        if (entry.externalId() != null) {
            return parseSuwayomiMangaId(entry.externalId());
        }
        return findStoredMangaInBothTrailingSlashForms(suwayomiSourceId, entry.url())
                .map(SuwayomiMangaDto::id)
                .orElseThrow(() -> new IllegalStateException("Suwayomi doesn't know the work at " + entry.url()));
    }

    // Suwayomi compares the address exactly, and the URLs we store lose their trailing slash when normalized, so the
    // work is looked up as given and then in the other form. The second lookup only happens if the first finds nothing.
    private Optional<SuwayomiMangaDto> findStoredMangaInBothTrailingSlashForms(String suwayomiSourceId, String url) {
        String otherForm = url.endsWith("/") ? url.substring(0, url.length() - 1) : url + "/";
        return Stream.of(url, otherForm)
                .map(form -> client.findStoredManga(suwayomiSourceId, form))
                .flatMap(Optional::stream)
                .findFirst();
    }

    private String resolveSuwayomiSourceId(ComicReadingSourceTarget source) {
        if (source.externalId() != null) {
            return source.externalId();
        }
        return findSuwayomiSourceByUrl(source.url())
                .map(SuwayomiSourceDto::id)
                .orElseThrow(() -> new IllegalStateException("Suwayomi has no source for " + source.url()));
    }

    // One limiter per Suwayomi source, created the first time it's needed from the shared config: every site has its
    // own limits, and a slow one mustn't hold back the others.
    private RateLimiterExecutor rateLimiterOfSuwayomiSource(String suwayomiSourceId) {
        return new RateLimiterExecutor(rateLimiterRegistry.rateLimiter(RATE_LIMITER_NAME_PREFIX + suwayomiSourceId,
                RATE_LIMITER_CONFIG_NAME));
    }

    private Optional<SuwayomiSourceDto> findSuwayomiSourceByUrl(String url) {
        String host = hostOfUrl(url);
        if (host == null) {
            return Optional.empty();
        }
        return listSuwayomiSources().stream()
                .filter(suwayomiSource -> host.equals(hostOfUrl(suwayomiSource.homeUrl())))
                .findFirst();
    }

    private static String hostOfUrl(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // No synchronization: this runs on virtual threads, so at worst two concurrent callers both fetch the list once
    // and the later write wins, which is harmless.
    private List<SuwayomiSourceDto> listSuwayomiSources() {
        Instant now = Instant.now();
        SuwayomiSourcesSnapshot cached = suwayomiSourcesCache.get();
        if (cached != null && now.isBefore(cached.fetchedAt().plus(suwayomiSourcesTtl))) {
            return cached.suwayomiSources();
        }
        // A source without a home URL (the "Local source") isn't a site to read from, so it's left out entirely.
        List<SuwayomiSourceDto> suwayomiSources = client.fetchSources()
                .stream()
                .filter(suwayomiSource -> suwayomiSource.homeUrl() != null && !suwayomiSource.homeUrl().isBlank())
                .toList();
        suwayomiSourcesCache.set(new SuwayomiSourcesSnapshot(now, suwayomiSources));
        return suwayomiSources;
    }

    private record SuwayomiSourcesSnapshot(Instant fetchedAt, List<SuwayomiSourceDto> suwayomiSources) {
    }
}
