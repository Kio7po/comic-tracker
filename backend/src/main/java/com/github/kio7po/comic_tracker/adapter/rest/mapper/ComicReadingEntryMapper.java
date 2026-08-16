package com.github.kio7po.comic_tracker.adapter.rest.mapper;

import java.util.List;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingEntryResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceResponseDto;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;

public final class ComicReadingEntryMapper {

    private ComicReadingEntryMapper() {
    }

    public static List<ComicReadingEntryResponseDto> toResponseDtoList(List<ComicReadingEntry> entries) {
        return entries.stream().map(ComicReadingEntryMapper::toResponseDto).toList();
    }

    public static ComicReadingEntryResponseDto toResponseDto(ComicReadingEntry entry) {
        return new ComicReadingEntryResponseDto(entry.getId(), entry.getUrl(), entry.getTitle(),
                entry.getAvailableChapters(), entry.getLocale(), entry.getStatus(),
                toSourceResponseDto(entry.getSource()), entry.getCreatedAt());
    }

    public static ComicReadingSourceResponseDto toSourceResponseDto(ComicReadingSource source) {
        return new ComicReadingSourceResponseDto(source.getId(), source.getSlug(), source.getName(),
                source.getUrl(), source.getIconUrl(), source.getStatus());
    }

    public static List<ComicReadingSourceResponseDto> toSourceResponseDtoList(List<ComicReadingSource> sources) {
        return sources.stream().map(ComicReadingEntryMapper::toSourceResponseDto).toList();
    }

}
