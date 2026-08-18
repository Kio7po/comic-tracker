package com.github.kio7po.comic_tracker.adapter.rest.dto;

// Wraps the plain ComicReadingSourceResponseDto instead of adding contributedBy to it directly:
// that DTO is also returned by the public, unauthenticated /api/reading-sources picker endpoint,
// which must not expose who submitted each source.
public record ComicReadingSourceModerationResponseDto(ComicReadingSourceResponseDto source,
        ContributorSummaryResponseDto contributedBy) {
}