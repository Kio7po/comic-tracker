package com.github.kio7po.comic_tracker.adapter.rest.dto;

import com.github.kio7po.comic_tracker.domain.enums.ReadingStateStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReadingStateRequestDto(@NotNull ReadingStateStatus status, @Min(0) int chapters) {
}
