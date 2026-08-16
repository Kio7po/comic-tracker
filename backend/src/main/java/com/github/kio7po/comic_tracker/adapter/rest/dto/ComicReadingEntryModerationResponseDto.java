package com.github.kio7po.comic_tracker.adapter.rest.dto;

// Wraps the plain ComicReadingEntryResponseDto instead of duplicating its fields: only the
// cross-comic moderation listing needs to say which Comic each entry belongs to (the per-comic
// endpoint's caller already knows it from the URL slug).
public record ComicReadingEntryModerationResponseDto(ComicReadingEntryResponseDto entry, ComicSummaryResponseDto comic) {
}