package com.github.kio7po.comic_tracker.adapter.rest.dto;

public record ComicReadingSearchResultResponseDto(
        String externalId,
        String title,
        String url,
        String coverUrl) {
}
