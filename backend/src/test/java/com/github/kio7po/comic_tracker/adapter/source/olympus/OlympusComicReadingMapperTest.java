package com.github.kio7po.comic_tracker.adapter.source.olympus;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDetails;

class OlympusComicReadingMapperTest {

    @Test
    void toSearchResult_buildsTheComicPageUrlFromTheSlug() {
        OlympusMangaDto dto = new OlympusMangaDto("Berserk", "berserk", "comic");

        ComicReadingSearchResult result = OlympusComicReadingMapper.toSearchResult(dto, "https://olympusxyz.com");

        assertThat(result.title()).isEqualTo("Berserk");
        assertThat(result.url()).isEqualTo("https://olympusxyz.com/series/comic-berserk");
    }

    @Test
    void toDetails_mapsTitleTotalChaptersAndLastChapterAtFromTheNewestChapter() {
        OlympusMangaDto manga = new OlympusMangaDto("Berserk", "berserk", "comic");
        OlympusChapterListResponseDto chapters = new OlympusChapterListResponseDto(
                List.of(new OlympusChapterDto("2024-06-01T10:00:00.000000Z"),
                        new OlympusChapterDto("2024-05-01T10:00:00.000000Z")),
                new OlympusChapterMetaDto(374));

        ComicReadingSourceDetails details = OlympusComicReadingMapper.toDetails(manga, chapters);

        assertThat(details.title()).isEqualTo("Berserk");
        assertThat(details.availableChapters()).isEqualTo(374);
        assertThat(details.lastChapterAt()).isEqualTo(Instant.parse("2024-06-01T10:00:00.000000Z"));
    }

    @Test
    void toDetails_leavesLastChapterAtNullWhenTheChapterListIsEmpty() {
        OlympusMangaDto manga = new OlympusMangaDto("Berserk", "berserk", "comic");
        OlympusChapterListResponseDto chapters = new OlympusChapterListResponseDto(List.of(), new OlympusChapterMetaDto(0));

        ComicReadingSourceDetails details = OlympusComicReadingMapper.toDetails(manga, chapters);

        assertThat(details.availableChapters()).isZero();
        assertThat(details.lastChapterAt()).isNull();
    }

    @Test
    void toDetails_leavesLastChapterAtNullWhenThePublishedDateIsUnparseable() {
        OlympusMangaDto manga = new OlympusMangaDto("Berserk", "berserk", "comic");
        OlympusChapterListResponseDto chapters = new OlympusChapterListResponseDto(
                List.of(new OlympusChapterDto("not a date")), new OlympusChapterMetaDto(1));

        ComicReadingSourceDetails details = OlympusComicReadingMapper.toDetails(manga, chapters);

        assertThat(details.lastChapterAt()).isNull();
    }

    @Test
    void toDetails_handlesANullChaptersResponseByLeavingChapterFieldsNull() {
        OlympusMangaDto manga = new OlympusMangaDto("Berserk", "berserk", "comic");

        ComicReadingSourceDetails details = OlympusComicReadingMapper.toDetails(manga, null);

        assertThat(details.title()).isEqualTo("Berserk");
        assertThat(details.availableChapters()).isNull();
        assertThat(details.lastChapterAt()).isNull();
    }
}
