package com.github.kio7po.comic_tracker.adapter.rest.dto;

import org.jspecify.annotations.Nullable;

import com.github.kio7po.comic_tracker.adapter.rest.dto.validation.NotBlankOrNull;
import com.github.kio7po.comic_tracker.domain.enums.ReadingStateStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReadingStateRequestDto(
        @NotNull ReadingStateStatus status,
        @Min(0) int chapters,
        @Nullable @NotBlankOrNull @Size(max = 2048) String notes) {
}
