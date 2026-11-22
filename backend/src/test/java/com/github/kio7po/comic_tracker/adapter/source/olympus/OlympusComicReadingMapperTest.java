package com.github.kio7po.comic_tracker.adapter.source.olympus;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;

class OlympusComicReadingMapperTest {

    private static final String BASE_URL = "https://olympusxyz.com";

    @Test
    void toSearchResult_buildsTheComicPageUrlFromTheSlugAndUsesTheIdAsExternalId() {
        OlympusMangaDto dto = new OlympusMangaDto(42L, "Berserk", "berserk", "comic", null, null);

        ComicReadingSearchResult result = OlympusComicReadingMapper.toSearchResult(dto, BASE_URL);

        assertThat(result.externalId()).isEqualTo("42");
        assertThat(result.title()).isEqualTo("Berserk");
        assertThat(result.url()).isEqualTo("https://olympusxyz.com/series/comic-berserk");
    }

    @Test
    void toSearchResult_leavesTheExternalIdNullWhenTheSeriesHasNoId() {
        OlympusMangaDto dto = new OlympusMangaDto(null, "Berserk", "berserk", "comic", null, null);

        ComicReadingSearchResult result = OlympusComicReadingMapper.toSearchResult(dto, BASE_URL);

        assertThat(result.externalId()).isNull();
    }

    @Test
    void toDetails_mapsIdUrlTitleTotalChaptersAndLatestChapterAtFromTheNewestChapter() {
        OlympusMangaDto manga = new OlympusMangaDto(42L, "Berserk", "berserk", "comic", null, null);
        OlympusChapterListResponseDto chapters = new OlympusChapterListResponseDto(
                List.of(new OlympusChapterDto("2024-06-01T10:00:00.000000Z"),
                        new OlympusChapterDto("2024-05-01T10:00:00.000000Z")),
                new OlympusChapterMetaDto(374));

        ComicReadingEntryDetails details = OlympusComicReadingMapper.toDetails(manga, chapters, BASE_URL);

        assertThat(details.externalId()).isEqualTo("42");
        assertThat(details.url()).isEqualTo("https://olympusxyz.com/series/comic-berserk");
        assertThat(details.title()).isEqualTo("Berserk");
        assertThat(details.availableChapters()).isEqualTo(374);
        assertThat(details.latestChapterAt()).isEqualTo(Instant.parse("2024-06-01T10:00:00.000000Z"));
    }

    @Test
    void toDetails_buildsTheUrlFromTheCurrentSlug() {
        OlympusMangaDto manga = new OlympusMangaDto(42L, "Berserk", "berserk-renamed", "comic", null, null);

        ComicReadingEntryDetails details = OlympusComicReadingMapper.toDetails(manga, null, BASE_URL);

        assertThat(details.url()).isEqualTo("https://olympusxyz.com/series/comic-berserk-renamed");
    }

    @Test
    void toDetails_leavesLatestChapterAtNullWhenTheChapterListIsEmpty() {
        OlympusMangaDto manga = new OlympusMangaDto(42L, "Berserk", "berserk", "comic", null, null);
        OlympusChapterListResponseDto chapters = new OlympusChapterListResponseDto(List.of(), new OlympusChapterMetaDto(0));

        ComicReadingEntryDetails details = OlympusComicReadingMapper.toDetails(manga, chapters, BASE_URL);

        assertThat(details.availableChapters()).isZero();
        assertThat(details.latestChapterAt()).isNull();
    }

    @Test
    void toDetails_leavesLatestChapterAtNullWhenThePublishedDateIsUnparseable() {
        OlympusMangaDto manga = new OlympusMangaDto(42L, "Berserk", "berserk", "comic", null, null);
        OlympusChapterListResponseDto chapters = new OlympusChapterListResponseDto(
                List.of(new OlympusChapterDto("not a date")), new OlympusChapterMetaDto(1));

        ComicReadingEntryDetails details = OlympusComicReadingMapper.toDetails(manga, chapters, BASE_URL);

        assertThat(details.latestChapterAt()).isNull();
    }

    @Test
    void toDetails_mapsTheSynopsisAndTheStatusAndLeavesTheAuthorNull() {
        OlympusMangaDto manga = new OlympusMangaDto(42L, "Berserk", "berserk", "comic", "A mercenary and his sword",
                new OlympusMangaStatusDto(4, "Finalizado"));

        ComicReadingEntryDetails details = OlympusComicReadingMapper.toDetails(manga, null, BASE_URL);

        assertThat(details.synopsis()).isEqualTo("A mercenary and his sword");
        assertThat(details.status()).isEqualTo(ComicStatus.COMPLETED);
        assertThat(details.author()).isNull();
    }

    @Test
    void toDetails_leavesTheSynopsisNullWhenItIsBlank() {
        OlympusMangaDto manga = new OlympusMangaDto(42L, "Berserk", "berserk", "comic", "   ", null);

        assertThat(OlympusComicReadingMapper.toDetails(manga, null, BASE_URL).synopsis()).isNull();
    }

    // Los ids son los que se han visto en Olympus. El 7 («Abandonado por el scan») es un estado conocido sin
    // equivalente exacto, así que es OTHER; un id que no conocemos es null, no OTHER.
    @ParameterizedTest(name = "status id {0} -> {1}")
    @CsvSource({ "1,ONGOING", "3,HIATUS", "4,COMPLETED", "5,CANCELLED", "7,OTHER", "99," })
    void toComicStatus_mapsByTheStatusIdAndLeavesUnknownIdsNull(Integer statusId, ComicStatus expected) {
        assertThat(OlympusComicReadingMapper.toComicStatus(new OlympusMangaStatusDto(statusId, "a label")))
                .isEqualTo(expected);
    }

    @Test
    void toComicStatus_doesNotLookAtTheLabel() {
        assertThat(OlympusComicReadingMapper.toComicStatus(new OlympusMangaStatusDto(4, "Otra etiqueta")))
                .isEqualTo(ComicStatus.COMPLETED);
    }

    @Test
    void toComicStatus_isNullWhenOlympusGivesNoStatus() {
        assertThat(OlympusComicReadingMapper.toComicStatus(null)).isNull();
        assertThat(OlympusComicReadingMapper.toComicStatus(new OlympusMangaStatusDto(null, "Activo"))).isNull();
    }

    @Test
    void toDetails_handlesANullChaptersResponseByLeavingChapterFieldsNull() {
        OlympusMangaDto manga = new OlympusMangaDto(42L, "Berserk", "berserk", "comic", null, null);

        ComicReadingEntryDetails details = OlympusComicReadingMapper.toDetails(manga, null, BASE_URL);

        assertThat(details.title()).isEqualTo("Berserk");
        assertThat(details.availableChapters()).isNull();
        assertThat(details.latestChapterAt()).isNull();
    }
}
