package com.github.kio7po.comic_tracker.adapter.metadata;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataProvider;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;

@Component
public class TenraiComicMetadataProvider implements ComicMetadataProvider {

    private static final String SLUG = "myanimelist";

    private final RestClient restClient;

    public TenraiComicMetadataProvider(RestClient.Builder restClientBuilder,
            @Value("${tenrai.api.base-url:https://api.tenrai.org/v1}") String baseUrl) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Page<ComicMetadataResult> search(String keywords, int limit, int offset, NsfwRating nsfw, ComicStatus status,
            ComicMediaType type) {
        int page = (offset / limit) + 1;

        TenraiMangaSearchResponseDto response = restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/manga")
                            .queryParam("q", keywords)
                            .queryParam("page", page)
                            .queryParam("limit", limit);
                    applyNsfwFilter(uriBuilder, nsfw);
                    TenraiComicMapper.toTenraiStatus(status).ifPresent(value -> uriBuilder.queryParam("status", value));
                    TenraiComicMapper.toTenraiType(type).ifPresent(value -> uriBuilder.queryParam("type", value));
                    return uriBuilder.build();
                })
                .retrieve()
                .body(TenraiMangaSearchResponseDto.class);

        if (response == null || response.data() == null) {
            return new Page<>(List.of(), false);
        }

        List<ComicMetadataResult> items = response.data().stream()
                .map(dto -> TenraiComicMapper.toResult(dto, SLUG))
                .toList();
        boolean existMoreItems = response.pagination() != null && response.pagination().hasNextPage();
        return new Page<>(items, existMoreItems);
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
            TenraiMangaResponseDto response = restClient.get()
                    .uri("/manga/{id}", externalId)
                    .retrieve()
                    .body(TenraiMangaResponseDto.class);
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
