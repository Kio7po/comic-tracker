package com.github.kio7po.comic_tracker.adapter.rest.dto;

import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;

public record ComicSearchResultResponseDto(
        String sourceSlug,
        String externalId,
        String title,
        String synopsis,
        String coverUrl,
        ComicMediaType mediaType,
        ComicStatus status,
        NsfwRating nsfw) {
}
