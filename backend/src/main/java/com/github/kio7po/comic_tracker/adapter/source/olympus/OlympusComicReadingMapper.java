package com.github.kio7po.comic_tracker.adapter.source.olympus;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverLocation;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;

final class OlympusComicReadingMapper {

    private OlympusComicReadingMapper() {
    }

    static ComicReadingSearchResult toSearchResult(OlympusMangaDto dto, String baseUrl) {
        return new ComicReadingSearchResult(externalId(dto), dto.name(), mangaUrl(baseUrl, dto.slug()),
                toCoverLocation(dto.cover()));
    }

    // La portada de Olympus es una URL pública de su CDN, así que se entrega tal cual.
    private static ComicReadingCoverLocation toCoverLocation(String cover) {
        return cover == null || cover.isBlank() ? null : new ComicReadingCoverLocation.PublicUrl(cover);
    }

    static String externalId(OlympusMangaDto dto) {
        return dto.id() == null ? null : dto.id().toString();
    }

    static String mangaUrl(String baseUrl, String slug) {
        return "%s/series/comic-%s".formatted(baseUrl, slug);
    }

    static ComicReadingEntryDetails toDetails(OlympusMangaDto manga, OlympusChapterListResponseDto chapters,
            String baseUrl) {
        Integer availableChapters = chapters == null || chapters.meta() == null ? null : chapters.meta().total();
        Instant latestChapterAt = latestChapterAt(chapters == null ? null : chapters.data());
        // Olympus no da autor de la obra: solo el equipo de scanlation que la publica.
        return new ComicReadingEntryDetails(externalId(manga), mangaUrl(baseUrl, manga.slug()), manga.name(),
                availableChapters, latestChapterAt, null, toComicStatus(manga.status()),
                manga.summary() == null || manga.summary().isBlank() ? null : manga.summary());
    }

    // Se mapea por id y no por la etiqueta en español, que puede cambiar. Los ids vistos en Olympus son 1 Activo,
    // 3 Pausado por el autor (Hiatus), 4 Finalizado, 5 Cancelado por el autor y 7 Abandonado por el scan, que es un
    // estado conocido sin equivalente exacto. Un id que no está aquí es un estado que no se conoce: null.
    static ComicStatus toComicStatus(OlympusMangaStatusDto status) {
        if (status == null || status.id() == null) {
            return null;
        }
        return switch (status.id()) {
            case 1 -> ComicStatus.ONGOING;
            case 3 -> ComicStatus.HIATUS;
            case 4 -> ComicStatus.COMPLETED;
            case 5 -> ComicStatus.CANCELLED;
            case 7 -> ComicStatus.OTHER;
            default -> null;
        };
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
