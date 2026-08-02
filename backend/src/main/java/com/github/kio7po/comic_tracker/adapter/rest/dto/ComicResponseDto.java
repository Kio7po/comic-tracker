package com.github.kio7po.comic_tracker.adapter.rest.dto;

import java.time.LocalDate;
import java.util.Set;

import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;

public record ComicResponseDto(
        Long id,
        String slug,
        String title,
        String synopsis,
        String coverUrl,
        Set<String> alternativeTitles,
        LocalDate startDate,
        LocalDate endDate,
        NsfwRating nsfw,
        ComicMediaType mediaType,
        ComicStatus status,
        Integer chapters,
        Set<String> authors,
        Set<String> genres,
        Set<String> tags) {
}
