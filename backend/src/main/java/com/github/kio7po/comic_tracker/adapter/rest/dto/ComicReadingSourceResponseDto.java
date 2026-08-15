package com.github.kio7po.comic_tracker.adapter.rest.dto;

import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;

public record ComicReadingSourceResponseDto(
        Long id,
        String slug,
        String name,
        String url,
        String iconUrl,
        ComicReadingSourceStatus status) {
}
