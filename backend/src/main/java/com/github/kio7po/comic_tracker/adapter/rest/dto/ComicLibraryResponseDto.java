package com.github.kio7po.comic_tracker.adapter.rest.dto;

import java.time.LocalDate;
import java.util.Set;

import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;

// Just the fields the personal library listing filters/sorts/displays by - synopsis and the
// authors/genres/tags relations stay out since nothing there reads them yet.
public record ComicLibraryResponseDto(
        Long id,
        String slug,
        String title,
        Set<String> alternativeTitles,
        String coverUrl,
        LocalDate startDate,
        NsfwRating nsfw,
        ComicMediaType mediaType,
        ComicStatus status,
        Integer chapters) {
}
