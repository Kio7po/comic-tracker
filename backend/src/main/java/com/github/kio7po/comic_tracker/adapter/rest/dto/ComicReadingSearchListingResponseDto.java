package com.github.kio7po.comic_tracker.adapter.rest.dto;

import java.util.List;

public record ComicReadingSearchListingResponseDto(
        String providerKey,
        List<ComicReadingSearchResultResponseDto> results) {
}
