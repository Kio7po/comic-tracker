package com.github.kio7po.comic_tracker.adapter.rest.dto;

import com.github.kio7po.comic_tracker.adapter.rest.dto.validation.ValidLocale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ComicReadingEntryFromSearchResultRequestDto(
        @NotNull Long sourceId,
        @NotBlank @Size(max = 64) String providerKey,
        @NotBlank @Size(max = 255) String externalId,
        @NotBlank @Size(max = 35) @ValidLocale String locale) {
}
