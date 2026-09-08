package com.github.kio7po.comic_tracker.adapter.source.olympus;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDetails;

final class OlympusComicReadingMapper {

    private OlympusComicReadingMapper() {
    }

    static ComicReadingSearchResult toSearchResult(OlympusMangaDto dto, String baseUrl) {
        return new ComicReadingSearchResult(dto.name(), mangaUrl(baseUrl, dto.slug()));
    }

    static String mangaUrl(String baseUrl, String slug) {
        return "%s/series/comic-%s".formatted(baseUrl, slug);
    }

    static ComicReadingSourceDetails toDetails(OlympusMangaDto manga, OlympusChapterListResponseDto chapters) {
        Integer availableChapters = chapters == null || chapters.meta() == null ? null : chapters.meta().total();
        Instant latestChapterAt = latestChapterAt(chapters == null ? null : chapters.data());
        return new ComicReadingSourceDetails(manga.name(), availableChapters, latestChapterAt);
    }

    private static Instant latestChapterAt(List<OlympusChapterDto> chapterList) {
        if (chapterList == null || chapterList.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(chapterList.get(0).publishedAt());
        } catch (DateTimeParseException | NullPointerException e) {
            return null;
        }
    }
}
