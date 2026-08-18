package com.github.kio7po.comic_tracker.adapter.rest.dto;

import org.hibernate.validator.constraints.URL;
import org.jspecify.annotations.Nullable;

import com.github.kio7po.comic_tracker.adapter.rest.dto.validation.NotBlankOrNull;
import com.github.kio7po.comic_tracker.adapter.rest.dto.validation.ValidLocale;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComicReadingEntryRequestDto(
        @Nullable Long sourceId,
        @Nullable @NotBlankOrNull @Size(max = 255) String sourceName,
        @Nullable @NotBlankOrNull @URL @Size(max = 255) String sourceUrl,
        @NotBlank @URL @Size(max = 255) String url,
        @NotBlank @Size(max = 35) @ValidLocale String locale) {

    @AssertTrue(message = "provide either sourceId, or both sourceName and sourceUrl, but not a mix")
    boolean isSourceSelectionValid() {
        boolean hasSourceId = sourceId != null;
        boolean hasSourceName = sourceName != null;
        boolean hasSourceUrl = sourceUrl != null;
        return hasSourceId ? (!hasSourceName && !hasSourceUrl) : (hasSourceName && hasSourceUrl);
    }

}
