package com.github.kio7po.comic_tracker.adapter.rest.mapper;

import java.util.List;

import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingEntryModerationResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingEntryResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingEntrySummaryResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceModerationResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicReadingSourceResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ComicSummaryResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.ContributorSummaryResponseDto;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;

public final class ComicReadingEntryMapper {

    private ComicReadingEntryMapper() {
    }

    public static List<ComicReadingEntryResponseDto> toResponseDtoList(List<ComicReadingEntry> entries) {
        return entries.stream().map(ComicReadingEntryMapper::toResponseDto).toList();
    }

    public static ComicReadingEntryResponseDto toResponseDto(ComicReadingEntry entry) {
        return new ComicReadingEntryResponseDto(entry.getId(), entry.getUrl(), entry.getTitle(),
                entry.getAvailableChapters(), entry.getLatestChapterAt(), entry.getLocale(), entry.getStatus(),
                toSourceResponseDto(entry.getSource()), entry.getCreatedAt());
    }

    public static ComicReadingEntrySummaryResponseDto toSummaryResponseDto(ComicReadingEntry entry) {
        return new ComicReadingEntrySummaryResponseDto(entry.getId(), entry.getUrl());
    }

    public static ComicReadingSourceResponseDto toSourceResponseDto(ComicReadingSource source) {
        return new ComicReadingSourceResponseDto(source.getId(), source.getSlug(), source.getName(),
                source.getUrl(), source.getIconUrl(), source.getStatus(), source.getCreatedAt());
    }

    public static List<ComicReadingSourceResponseDto> toSourceResponseDtoList(List<ComicReadingSource> sources) {
        return sources.stream().map(ComicReadingEntryMapper::toSourceResponseDto).toList();
    }

    public static List<ComicReadingEntryModerationResponseDto> toModerationResponseDtoList(
            List<ComicReadingEntry> entries) {
        return entries.stream().map(ComicReadingEntryMapper::toModerationResponseDto).toList();
    }

    public static ComicReadingEntryModerationResponseDto toModerationResponseDto(ComicReadingEntry entry) {
        return new ComicReadingEntryModerationResponseDto(toResponseDto(entry),
                toComicSummaryResponseDto(entry.getComic()), toContributorSummaryResponseDto(entry.getContributedBy()));
    }

    public static ComicSummaryResponseDto toComicSummaryResponseDto(Comic comic) {
        return new ComicSummaryResponseDto(comic.getSlug(), comic.getTitle(), comic.getCoverUrl());
    }

    public static List<ComicReadingSourceModerationResponseDto> toSourceModerationResponseDtoList(
            List<ComicReadingSource> sources) {
        return sources.stream().map(ComicReadingEntryMapper::toSourceModerationResponseDto).toList();
    }

    public static ComicReadingSourceModerationResponseDto toSourceModerationResponseDto(ComicReadingSource source) {
        return new ComicReadingSourceModerationResponseDto(toSourceResponseDto(source),
                toContributorSummaryResponseDto(source.getContributedBy()));
    }

    public static ContributorSummaryResponseDto toContributorSummaryResponseDto(User user) {
        return new ContributorSummaryResponseDto(user.getUsername());
    }

}
