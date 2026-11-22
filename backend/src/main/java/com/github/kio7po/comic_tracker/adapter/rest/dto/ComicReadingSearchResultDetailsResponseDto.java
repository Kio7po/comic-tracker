package com.github.kio7po.comic_tracker.adapter.rest.dto;

import java.time.Instant;

import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;

public record ComicReadingSearchResultDetailsResponseDto(
        String title,
        String url,
        Integer availableChapters,
        Instant latestChapterAt,
        String author,
        ComicStatus status,
        String synopsis) {
}
