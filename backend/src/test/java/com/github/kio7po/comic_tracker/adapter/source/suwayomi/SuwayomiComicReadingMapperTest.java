package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;

class SuwayomiComicReadingMapperTest {

    private static final SuwayomiMangaDto MANGA = new SuwayomiMangaDto(7, "Solo Leveling",
            "https://www.leercapitulo.co/manga/wj10bv5s3y/solo-leveling/", null, null, null);

    @Test
    void toReadingSourceDescriptor_usesTheSourceIdNameAndHomeUrl() {
        SuwayomiSourceDto dto = new SuwayomiSourceDto("5621901596522308538", "LeerCapitulo", "es",
                "https://www.leercapitulo.co");

        ComicReadingSourceDescriptor descriptor = SuwayomiComicReadingMapper.toReadingSourceDescriptor(dto);

        assertThat(descriptor.externalId()).isEqualTo("5621901596522308538");
        assertThat(descriptor.name()).isEqualTo("LeerCapitulo");
        assertThat(descriptor.url()).isEqualTo("https://www.leercapitulo.co");
    }

    @Test
    void toReadingSearchResult_usesTheMangaIdAsExternalId() {
        ComicReadingSearchResult result = SuwayomiComicReadingMapper.toReadingSearchResult(MANGA);

        assertThat(result.externalId()).isEqualTo("7");
        assertThat(result.title()).isEqualTo("Solo Leveling");
        assertThat(result.url()).isEqualTo("https://www.leercapitulo.co/manga/wj10bv5s3y/solo-leveling/");
    }

    @Test
    void toReadingSearchResult_leavesTheUrlNullWhenSuwayomiHasNotFetchedTheWorkPage() {
        ComicReadingSearchResult result = SuwayomiComicReadingMapper
                .toReadingSearchResult(new SuwayomiMangaDto(7, "Solo Leveling", null, null, null, null));

        assertThat(result.url()).isNull();
    }

    @Test
    void toReadingEntryDetails_mapsIdUrlTitleChapterCountAndNewestUploadDate() {
        SuwayomiMangaWithChaptersDto dto = new SuwayomiMangaWithChaptersDto(MANGA,
                List.of(new SuwayomiChapterDto("1700000000000"), new SuwayomiChapterDto("1710000000000"),
                        new SuwayomiChapterDto("1705000000000")));

        ComicReadingEntryDetails details = SuwayomiComicReadingMapper.toReadingEntryDetails(dto);

        assertThat(details.externalId()).isEqualTo("7");
        assertThat(details.url()).isEqualTo(MANGA.realUrl());
        assertThat(details.title()).isEqualTo("Solo Leveling");
        assertThat(details.availableChapters()).isEqualTo(3);
        assertThat(details.latestChapterAt()).isEqualTo(Instant.ofEpochMilli(1710000000000L));
    }

    @Test
    void toReadingEntryDetails_ignoresChaptersWithoutUploadDateWhenLookingForTheNewest() {
        SuwayomiMangaWithChaptersDto dto = new SuwayomiMangaWithChaptersDto(MANGA,
                List.of(new SuwayomiChapterDto("0"), new SuwayomiChapterDto("1700000000000")));

        ComicReadingEntryDetails details = SuwayomiComicReadingMapper.toReadingEntryDetails(dto);

        assertThat(details.availableChapters()).isEqualTo(2);
        assertThat(details.latestChapterAt()).isEqualTo(Instant.ofEpochMilli(1700000000000L));
    }

    @Test
    void toReadingEntryDetails_leavesLatestChapterAtNullWhenNoChapterHasAnUploadDate() {
        SuwayomiMangaWithChaptersDto dto = new SuwayomiMangaWithChaptersDto(MANGA,
                List.of(new SuwayomiChapterDto("0")));

        assertThat(SuwayomiComicReadingMapper.toReadingEntryDetails(dto).latestChapterAt()).isNull();
    }

    @Test
    void toReadingEntryDetails_mapsAuthorSynopsisAndStatus() {
        SuwayomiMangaDto manga = new SuwayomiMangaDto(7, "Solo Leveling", MANGA.realUrl(), "Chugong",
                "A hunter story", "ONGOING");

        ComicReadingEntryDetails details = SuwayomiComicReadingMapper
                .toReadingEntryDetails(new SuwayomiMangaWithChaptersDto(manga, List.of()));

        assertThat(details.author()).isEqualTo("Chugong");
        assertThat(details.synopsis()).isEqualTo("A hunter story");
        assertThat(details.status()).isEqualTo(ComicStatus.ONGOING);
    }

    @Test
    void toReadingEntryDetails_leavesAuthorAndSynopsisNullWhenTheyAreBlank() {
        SuwayomiMangaDto manga = new SuwayomiMangaDto(7, "Solo Leveling", MANGA.realUrl(), "  ", "", "UNKNOWN");

        ComicReadingEntryDetails details = SuwayomiComicReadingMapper
                .toReadingEntryDetails(new SuwayomiMangaWithChaptersDto(manga, List.of()));

        assertThat(details.author()).isNull();
        assertThat(details.synopsis()).isNull();
        assertThat(details.status()).isNull();
    }

    // Una celda vacía en @CsvSource llega como null, que es lo que se espera de lo que no se sabe mapear.
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({ "ONGOING,ONGOING", "COMPLETED,COMPLETED", "ON_HIATUS,HIATUS", "CANCELLED,CANCELLED",
            "LICENSED,OTHER", "PUBLISHING_FINISHED,OTHER", "UNKNOWN,", "A_STATUS_WE_DONT_KNOW," })
    void toComicStatus_mapsEachSuwayomiStatusAndLeavesUnknownOnesNull(String suwayomiStatus,
            ComicStatus expected) {
        assertThat(SuwayomiComicReadingMapper.toComicStatus(suwayomiStatus)).isEqualTo(expected);
    }

    @Test
    void toComicStatus_isNullWhenSuwayomiGivesNoStatus() {
        assertThat(SuwayomiComicReadingMapper.toComicStatus(null)).isNull();
    }

    @Test
    void toReadingEntryDetails_reportsZeroChaptersAndNoDateForAWorkWithoutChapters() {
        ComicReadingEntryDetails details = SuwayomiComicReadingMapper
                .toReadingEntryDetails(new SuwayomiMangaWithChaptersDto(MANGA, List.of()));

        assertThat(details.availableChapters()).isZero();
        assertThat(details.latestChapterAt()).isNull();
    }
}
