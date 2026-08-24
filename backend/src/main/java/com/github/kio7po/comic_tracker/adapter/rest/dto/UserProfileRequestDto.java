package com.github.kio7po.comic_tracker.adapter.rest.dto;

import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

import com.github.kio7po.comic_tracker.adapter.rest.dto.validation.NotBlankOrNull;
import com.github.kio7po.comic_tracker.adapter.rest.dto.validation.ValidLocale;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileRequestDto(
        @NotBlank @Size(max = 255) String displayName,
        @Nullable @NotBlankOrNull @Size(max = 2048) String biography,
        @Nullable @NotBlankOrNull @URL @Size(max = 255) String pictureUrl,
        @Nullable @NotBlankOrNull @Size(max = 35) @ValidLocale String locale) {
}
