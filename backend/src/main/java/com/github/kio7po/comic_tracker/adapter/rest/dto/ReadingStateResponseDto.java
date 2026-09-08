package com.github.kio7po.comic_tracker.adapter.rest.dto;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.github.kio7po.comic_tracker.domain.enums.ReadingStateStatus;

public record ReadingStateResponseDto(
        Long id,
        ReadingStateStatus status,
        int chapters,
        String notes,
        @Nullable ComicReadingEntrySummaryResponseDto preferredEntry,
        Instant createdAt,
        Instant updatedAt) {
}
