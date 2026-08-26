package com.github.kio7po.comic_tracker.adapter.rest.dto;

import java.time.Instant;

import com.github.kio7po.comic_tracker.domain.enums.ReadingStateStatus;

public record ReadingStateResponseDto(
        Long id,
        ReadingStateStatus status,
        int chapters,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
