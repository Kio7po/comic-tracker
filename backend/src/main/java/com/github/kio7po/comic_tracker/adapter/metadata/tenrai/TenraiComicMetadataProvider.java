package com.github.kio7po.comic_tracker.adapter.metadata.tenrai;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.enums.ComicSearchSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataProvider;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;

@Component
public class TenraiComicMetadataProvider implements ComicMetadataProvider {

    private static final String SLUG = "myanimelist";

    // Tenrai hard-caps pagination at page 1000, requesting further pages returns an error
    // even when the response's own pagination metadata still claims more items exist.
    // existMoreItems/totalItems are clamped around this ceiling.
    private static final int MAX_PAGE = 1000;

    private final RestClient restClient;
    private final RateLimiter rpsLimiter;
    private final RateLimiter rpmLimiter;

    public TenraiComicMetadataProvider(RestClient.Builder restClientBuilder,
            @Value("${tenrai.api.base-url:https://api.tenrai.org/v1}") String baseUrl,
            RateLimiterRegistry rateLimiterRegistry) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.rpsLimiter = rateLimiterRegistry.rateLimiter("tenrai-rps");
        this.rpmLimiter = rateLimiterRegistry.rateLimiter("tenrai-rpm");
    }

    private <T> T rateLimited(Supplier<T> call) {
        return RateLimiter.decorateSupplier(rpmLimiter, RateLimiter.decorateSupplier(rpsLimiter, call)).get();
    }

    @Override
    public Page<ComicMetadataResult> search(String keywords, int limit, int offset, NsfwRating nsfw, ComicStatus status,
            ComicMediaType type, ComicSearchSortField sortBy, SortDirection direction) {
        int page = (offset / limit) + 1;

        TenraiMangaSearchResponseDto response = rateLimited(() -> restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/manga")
                            .queryParam("q", keywords)
                            .queryParam("page", page)
                            .queryParam("limit", limit);
                    applyNsfwFilter(uriBuilder, nsfw);
                    TenraiComicMapper.toTenraiStatus(status).ifPresent(value -> uriBuilder.queryParam("status", value));
                    TenraiComicMapper.toTenraiType(type).ifPresent(value -> uriBuilder.queryParam("type", value));
                    TenraiComicMapper.toTenraiOrderBy(sortBy)
                            .ifPresent(value -> uriBuilder.queryParam("order_by", value));
                    TenraiComicMapper.toTenraiSort(direction).ifPresent(value -> uriBuilder.queryParam("sort", value));
                    return uriBuilder.build();
                })
                .retrieve()
                .body(TenraiMangaSearchResponseDto.class));

        if (response == null || response.data() == null) {
            return new Page<>(List.of(), false, null);
        }

        List<ComicMetadataResult> items = response.data().stream()
                .map(dto -> TenraiComicMapper.toResult(dto, SLUG))
                .toList();
        boolean existMoreItems = page < MAX_PAGE
                && response.pagination() != null
                && response.pagination().hasNextPage();
        Integer totalItems = response.pagination() != null && response.pagination().items() != null
                ? Math.min(response.pagination().items().total(), MAX_PAGE * limit)
                : null;
        return new Page<>(items, existMoreItems, totalItems);
    }

    private static void applyNsfwFilter(UriBuilder uriBuilder, NsfwRating nsfw) {
        if (nsfw == null) {
            return;
        }
        switch (nsfw) {
            case NONE -> uriBuilder.queryParam("sfw-strict", "true");
            case SUGGESTIVE -> uriBuilder.queryParam("sfw", "true");
            case EXPLICIT -> {
                // sin filtro: se permite ver cualquier contenido
            }
        }
    }

    @Override
    public Optional<ComicMetadataResult> fetch(String externalId) {
        try {
            TenraiMangaResponseDto response = rateLimited(() -> restClient.get()
                    .uri("/manga/{id}", externalId)
                    .retrieve()
                    .body(TenraiMangaResponseDto.class));
            return Optional.ofNullable(response)
                    .map(TenraiMangaResponseDto::data)
                    .map(dto -> TenraiComicMapper.toResult(dto, SLUG));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public String getSourceSlug() {
        return SLUG;
    }

}
