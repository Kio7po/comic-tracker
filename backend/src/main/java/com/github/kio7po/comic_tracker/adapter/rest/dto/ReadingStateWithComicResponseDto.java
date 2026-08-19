package com.github.kio7po.comic_tracker.adapter.rest.dto;

// Wraps the plain ReadingStateResponseDto instead of duplicating its fields: only the cross-comic
// personal-tracking listing needs to say which Comic each ReadingState belongs to (the per-comic
// endpoint's caller already knows it from the URL slug).
public record ReadingStateWithComicResponseDto(ReadingStateResponseDto readingState, ComicSummaryResponseDto comic) {
}
