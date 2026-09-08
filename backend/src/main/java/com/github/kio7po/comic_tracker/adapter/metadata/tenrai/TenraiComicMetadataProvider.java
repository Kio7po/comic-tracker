package com.github.kio7po.comic_tracker.adapter.metadata.tenrai;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import com.github.kio7po.comic_tracker.adapter.common.RateLimiterExecutor;
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
    private final RateLimiterExecutor rateLimiter;

    public TenraiComicMetadataProvider(RestClient.Builder restClientBuilder,
            @Value("${tenrai.api.base-url:https://api.tenrai.org/v1}") String baseUrl,
            RateLimiterRegistry rateLimiterRegistry) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.rateLimiter = new RateLimiterExecutor(rateLimiterRegistry.rateLimiter("tenrai-rpm"),
                rateLimiterRegistry.rateLimiter("tenrai-rps"));
    }

    @Override
    public Page<ComicMetadataResult> search(String keywords, int limit, int offset, NsfwRating nsfw, ComicStatus status,
            ComicMediaType type, ComicSearchSortField sortBy, SortDirection direction) {
        Optional<String> tenraiStatus = TenraiComicMapper.toTenraiStatus(status);
        Optional<String> tenraiType = TenraiComicMapper.toTenraiType(type);
        // status/type with no Tenrai equivalent (e.g. ComicStatus.OTHER, ComicMediaType.WEBTOON)
        // can't be honored by the remote API at all - silently dropping the filter and returning
        // unfiltered results would misrepresent them as matching a filter they don't match.
        if ((status != null && tenraiStatus.isEmpty()) || (type != null && tenraiType.isEmpty())) {
            return new Page<>(List.of(), false, 0);
        }

        int page = (offset / limit) + 1;

        TenraiMangaSearchResponseDto response = rateLimiter.execute(() -> restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/manga")
                            .queryParam("q", keywords)
                            .queryParam("page", page)
                            .queryParam("limit", limit);
                    applyNsfwFilter(uriBuilder, nsfw);
                    tenraiStatus.ifPresent(value -> uriBuilder.queryParam("status", value));
                    tenraiType.ifPresent(value -> uriBuilder.queryParam("type", value));
                    Optional<String> orderBy = TenraiComicMapper.toTenraiOrderBy(sortBy);
                    orderBy.ifPresent(value -> uriBuilder.queryParam("order_by", value));
                    // "sort" (direction) is meaningless to Jikan without an accompanying order_by field.
                    if (orderBy.isPresent()) {
                        TenraiComicMapper.toTenraiSort(sortBy, direction)
                                .ifPresent(value -> uriBuilder.queryParam("sort", value));
                    }
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
            TenraiMangaResponseDto response = rateLimiter.execute(() -> restClient.get()
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
