package com.github.kio7po.comic_tracker.adapter.rest.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicSearchResultResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.PageResponseDto;
import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.entities.Author;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.Genre;
import com.github.kio7po.comic_tracker.domain.entities.Tag;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;

public final class CatalogMapper {

    private CatalogMapper() {
    }

    public static PageResponseDto<ComicSearchResultResponseDto> toSearchResultPageDto(
            Page<ComicMetadataResult> page) {
        List<ComicSearchResultResponseDto> items = page.getItems().stream()
                .map(CatalogMapper::toSearchResultDto)
                .toList();
        return new PageResponseDto<>(items, page.isExistMoreItems(), page.getTotalItems());
    }

    public static ComicSearchResultResponseDto toSearchResultDto(ComicMetadataResult result) {
        Comic comic = result.getComic();
        return new ComicSearchResultResponseDto(result.getSourceSlug(), result.getExternalId(), comic.getTitle(),
                comic.getSynopsis(), comic.getCoverUrl(), comic.getMediaType(), comic.getStatus(), comic.getNsfw());
    }

    public static ComicResponseDto toResponseDto(Comic comic) {
        return new ComicResponseDto(comic.getId(), comic.getSlug(), comic.getTitle(), comic.getSynopsis(),
                comic.getCoverUrl(), comic.getAlternativeTitles(), comic.getStartDate(), comic.getEndDate(),
                comic.getNsfw(), comic.getMediaType(), comic.getStatus(), comic.getChapters(),
                comic.getAuthors().stream().map(Author::getName).collect(Collectors.toSet()),
                comic.getGenres().stream().map(Genre::getName).collect(Collectors.toSet()),
                comic.getTags().stream().map(Tag::getName).collect(Collectors.toSet()));
    }

}
