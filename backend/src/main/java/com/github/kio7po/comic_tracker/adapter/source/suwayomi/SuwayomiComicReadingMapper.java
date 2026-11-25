package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverLocation;
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
                suwayomiManga.realUrl(), toCoverLocation(suwayomiManga));
    }

    // Suwayomi no da la dirección original de la imagen, solo una ruta propia: la portada solo se puede servir a
    // través de él. Sus portadas son por obra, así que el id de la portada es el id de la obra.
    private static ComicReadingCoverLocation toCoverLocation(SuwayomiMangaDto suwayomiManga) {
        String thumbnailUrl = suwayomiManga.thumbnailUrl();
        return thumbnailUrl == null || thumbnailUrl.isBlank() ? null
                : new ComicReadingCoverLocation.ServedByProvider(toExternalComicId(suwayomiManga));
    }

    static String toExternalComicId(SuwayomiMangaDto suwayomiManga) {
        return suwayomiManga.id().toString();
    }

    static ComicReadingEntryDetails toReadingEntryDetails(SuwayomiMangaWithChaptersDto suwayomiMangaWithChapters) {
        SuwayomiMangaDto suwayomiManga = suwayomiMangaWithChapters.manga();
        List<SuwayomiChapterDto> chapters = suwayomiMangaWithChapters.chapters();
        return new ComicReadingEntryDetails(toExternalComicId(suwayomiManga), suwayomiManga.realUrl(),
                suwayomiManga.title(), chapters.size(), latestChapterAt(chapters),
                nullIfBlank(suwayomiManga.author()), toComicStatus(suwayomiManga.status()),
                nullIfBlank(suwayomiManga.description()));
    }

    // "UNKNOWN" y lo que no reconocemos quedan en null: no se sabe. OTHER es solo para un estado que Suwayomi sí
    // informa y que no tiene equivalente en el nuestro.
    static ComicStatus toComicStatus(String suwayomiStatus) {
        if (suwayomiStatus == null) {
            return null;
        }
        return switch (suwayomiStatus) {
            case "ONGOING" -> ComicStatus.ONGOING;
            case "COMPLETED" -> ComicStatus.COMPLETED;
            case "ON_HIATUS" -> ComicStatus.HIATUS;
            case "CANCELLED" -> ComicStatus.CANCELLED;
            case "LICENSED", "PUBLISHING_FINISHED" -> ComicStatus.OTHER;
            default -> null;
        };
    }

    private static String nullIfBlank(String text) {
        return text == null || text.isBlank() ? null : text;
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
