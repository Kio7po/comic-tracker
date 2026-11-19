package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;

final class SuwayomiComicReadingMapper {

    private SuwayomiComicReadingMapper() {
    }

    static ComicReadingSourceDescriptor toReadingSourceDescriptor(SuwayomiSourceDto suwayomiSource) {
        return new ComicReadingSourceDescriptor(suwayomiSource.id(), suwayomiSource.name(),
                suwayomiSource.homeUrl());
    }

    static ComicReadingSearchResult toReadingSearchResult(SuwayomiMangaDto suwayomiManga) {
        return new ComicReadingSearchResult(toExternalComicId(suwayomiManga), suwayomiManga.title(),
                suwayomiManga.realUrl());
    }

    static String toExternalComicId(SuwayomiMangaDto suwayomiManga) {
        return suwayomiManga.id().toString();
    }

    static ComicReadingEntryDetails toReadingEntryDetails(SuwayomiMangaWithChaptersDto suwayomiMangaWithChapters) {
        SuwayomiMangaDto suwayomiManga = suwayomiMangaWithChapters.manga();
        List<SuwayomiChapterDto> chapters = suwayomiMangaWithChapters.chapters();
        return new ComicReadingEntryDetails(toExternalComicId(suwayomiManga), suwayomiManga.realUrl(),
                suwayomiManga.title(), chapters.size(), latestChapterAt(chapters));
    }

    // Suwayomi sends "0" when the site gives no upload date for a chapter.
    private static Instant latestChapterAt(List<SuwayomiChapterDto> chapters) {
        OptionalLong latest = chapters.stream()
                .map(SuwayomiChapterDto::uploadDate)
                .filter(Objects::nonNull)
                .mapToLong(Long::parseLong)
                .filter(millis -> millis > 0)
                .max();
        return latest.isPresent() ? Instant.ofEpochMilli(latest.getAsLong()) : null;
    }
}
