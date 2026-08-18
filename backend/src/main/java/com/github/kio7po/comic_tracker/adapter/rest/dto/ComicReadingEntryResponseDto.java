package com.github.kio7po.comic_tracker.adapter.rest.dto;

import java.time.Instant;

import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;

public record ComicReadingEntryResponseDto(
        Long id,
        String url,
        String title,
        Integer availableChapters,
        String locale,
        ComicReadingEntryStatus status,
        ComicReadingSourceResponseDto source,
        Instant createdAt) {
}
