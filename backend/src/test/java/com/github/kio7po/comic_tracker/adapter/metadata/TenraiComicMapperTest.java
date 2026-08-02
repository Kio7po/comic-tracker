package com.github.kio7po.comic_tracker.adapter.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.kio7po.comic_tracker.domain.entities.Author;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.Genre;
import com.github.kio7po.comic_tracker.domain.entities.Tag;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;

class TenraiComicMapperTest {

    private static final String SOURCE_SLUG = "some-source";

    private static TenraiNamedResourceDto named(String name) {
        return new TenraiNamedResourceDto(name);
    }

    private static TenraiMangaDto fullDto() {
        return new TenraiMangaDto(
                2L,
                "Berserk",
                "Berserk",
                "ベルセルク",
                List.of("Berserk: The Prototype"),
                "Guts is out for revenge.",
                new TenraiImagesDto(
                        new TenraiImagesDto.TenraiImageSetDto("small.jpg", "small.jpg", "large.jpg"),
                        new TenraiImagesDto.TenraiImageSetDto("small.webp", "small.webp", "large.webp")),
                "Manga",
                "Publishing",
                new TenraiPublishedDto(
                        OffsetDateTime.parse("1989-08-25T00:00:00+00:00"),
                        null),
                null,
                List.of(named("Miura, Kentarou")),
                List.of(named("Action"), named("Adventure")),
                List.of(),
                List.of(named("Gore"), named("Military")),
                List.of(named("Seinen")));
    }

    private static TenraiMangaDto fullDtoWithExplicitGenres(List<TenraiNamedResourceDto> explicitGenres) {
        TenraiMangaDto base = fullDto();
        return new TenraiMangaDto(
                base.malId(), base.title(), base.titleEnglish(), base.titleJapanese(), base.titleSynonyms(),
                base.synopsis(), base.images(), base.type(), base.status(), base.published(), base.chapters(),
                base.authors(), base.genres(), explicitGenres, base.themes(), base.demographics());
    }

    private static TenraiMangaDto dtoWithTypeAndStatus(String type, String status) {
        return new TenraiMangaDto(
                2L, "Berserk", null, null, null,
                null, null, type, status, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void toResult_mapsExternalIdToMalIdAsString() {
        ComicMetadataResult result = TenraiComicMapper.toResult(fullDto(), SOURCE_SLUG);

        assertThat(result.getExternalId()).isEqualTo("2");
    }

    @Test
    void toResult_mapsGivenSourceSlugAsIs() {
        ComicMetadataResult result = TenraiComicMapper.toResult(fullDto(), SOURCE_SLUG);

        assertThat(result.getSourceSlug()).isEqualTo(SOURCE_SLUG);
    }

    @Test
    void toResult_mapsBasicComicFields() {
        Comic comic = TenraiComicMapper.toResult(fullDto(), SOURCE_SLUG).getComic();

        assertThat(comic.getTitle()).isEqualTo("Berserk");
        assertThat(comic.getSynopsis()).isEqualTo("Guts is out for revenge.");
        assertThat(comic.getCoverUrl()).isEqualTo("large.jpg");
        assertThat(comic.getStartDate()).isEqualTo(LocalDate.of(1989, 8, 25));
        assertThat(comic.getEndDate()).isNull();
        assertThat(comic.getMediaType()).isEqualTo(ComicMediaType.MANGA);
        assertThat(comic.getStatus()).isEqualTo(ComicStatus.ONGOING);
        assertThat(comic.getChapters()).isNull();
    }

    @Test
    void toResult_combinesTitleEnglishJapaneseAndSynonymsIntoAlternativeTitles() {
        Comic comic = TenraiComicMapper.toResult(fullDto(), SOURCE_SLUG).getComic();

        assertThat(comic.getAlternativeTitles())
                .containsExactlyInAnyOrder("Berserk", "ベルセルク", "Berserk: The Prototype");
    }

    @Test
    void toResult_ignoresNullOrBlankAlternativeTitles() {
        TenraiMangaDto dto = new TenraiMangaDto(
                2L, "Berserk", null, "  ", List.of("", " ", "Real Synonym"),
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of());

        Comic comic = TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic();

        assertThat(comic.getAlternativeTitles()).containsExactly("Real Synonym");
    }

    @Test
    void toResult_mapsAuthorsToAuthorSetByName() {
        Comic comic = TenraiComicMapper.toResult(fullDto(), SOURCE_SLUG).getComic();

        assertThat(comic.getAuthors())
                .extracting(Author::getName)
                .containsExactly("Miura, Kentarou");
    }

    @Test
    void toResult_mergesGenresAndExplicitGenresIntoOneGenreSet() {
        TenraiMangaDto dto = fullDtoWithExplicitGenres(List.of(named("Hentai")));

        Comic comic = TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic();

        assertThat(comic.getGenres())
                .extracting(Genre::getName)
                .containsExactlyInAnyOrder("Action", "Adventure", "Hentai");
    }

    @Test
    void toResult_mergesThemesAndDemographicsIntoOneTagSet() {
        Comic comic = TenraiComicMapper.toResult(fullDto(), SOURCE_SLUG).getComic();

        assertThat(comic.getTags())
                .extracting(Tag::getName)
                .containsExactlyInAnyOrder("Gore", "Military", "Seinen");
    }

    @Test
    void toResult_coverUrlIsNullWhenImagesAreMissing() {
        TenraiMangaDto dto = new TenraiMangaDto(
                2L, "Berserk", null, null, null,
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic().getCoverUrl()).isNull();
    }

    @Test
    void toResult_coverUrlIsNullWhenJpgIsMissing() {
        TenraiMangaDto dto = new TenraiMangaDto(
                2L, "Berserk", null, null, null,
                null, new TenraiImagesDto(null, null), null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of());

        assertThat(TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic().getCoverUrl()).isNull();
    }

    @Test
    void toResult_datesAreNullWhenPublishedIsMissing() {
        TenraiMangaDto dto = new TenraiMangaDto(
                2L, "Berserk", null, null, null,
                null, null, null, null, null, null,
                List.of(), List.of(), List.of(), List.of(), List.of());

        Comic comic = TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic();

        assertThat(comic.getStartDate()).isNull();
        assertThat(comic.getEndDate()).isNull();
    }

    @Test
    void toResult_nsfwIsExplicitWhenExplicitGenresPresent() {
        TenraiMangaDto dto = fullDtoWithExplicitGenres(List.of(named("Hentai")));

        assertThat(TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic().getNsfw()).isEqualTo(NsfwRating.EXPLICIT);
    }

    @Test
    void toResult_nsfwIsNoneWhenNoExplicitGenres() {
        assertThat(TenraiComicMapper.toResult(fullDto(), SOURCE_SLUG).getComic().getNsfw()).isEqualTo(NsfwRating.NONE);
    }

    @ParameterizedTest
    @CsvSource({
            "Manga, MANGA",
            "Novel, NOVEL",
            "Light Novel, NOVEL",
            "One-shot, ONE_SHOT",
            "Doujinshi, DOUJINSHI",
            "Manhwa, MANHWA",
            "Manhua, MANHUA",
            "Some Unknown Type, OTHER"
    })
    void toResult_mapsTenraiTypeToMediaType(String tenraiType, ComicMediaType expected) {
        TenraiMangaDto dto = dtoWithTypeAndStatus(tenraiType, null);

        assertThat(TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic().getMediaType()).isEqualTo(expected);
    }

    @Test
    void toResult_mediaTypeIsNullWhenTypeIsMissing() {
        TenraiMangaDto dto = dtoWithTypeAndStatus(null, null);

        assertThat(TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic().getMediaType()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "Publishing, ONGOING",
            "Finished, COMPLETED",
            "On Hiatus, HIATUS",
            "Discontinued, CANCELLED",
            "Not yet published, OTHER"
    })
    void toResult_mapsTenraiStatusToComicStatus(String tenraiStatus, ComicStatus expected) {
        TenraiMangaDto dto = dtoWithTypeAndStatus(null, tenraiStatus);

        assertThat(TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic().getStatus()).isEqualTo(expected);
    }

    @Test
    void toResult_statusIsNullWhenStatusIsMissing() {
        TenraiMangaDto dto = dtoWithTypeAndStatus(null, null);

        assertThat(TenraiComicMapper.toResult(dto, SOURCE_SLUG).getComic().getStatus()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "ONGOING, publishing",
            "COMPLETED, complete",
            "HIATUS, hiatus",
            "CANCELLED, discontinued"
    })
    void toTenraiStatus_mapsStatusesWithEquivalent(ComicStatus status, String expected) {
        assertThat(TenraiComicMapper.toTenraiStatus(status)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = "OTHER")
    @NullSource
    void toTenraiStatus_returnsEmptyWhenNoEquivalent(ComicStatus status) {
        assertThat(TenraiComicMapper.toTenraiStatus(status)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "MANGA, manga",
            "NOVEL, novel",
            "ONE_SHOT, oneshot",
            "DOUJINSHI, doujin",
            "MANHWA, manhwa",
            "MANHUA, manhua"
    })
    void toTenraiType_mapsTypesWithEquivalent(ComicMediaType type, String expected) {
        assertThat(TenraiComicMapper.toTenraiType(type)).contains(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = { "WEBTOON", "COMIC", "OTHER" })
    @NullSource
    void toTenraiType_returnsEmptyWhenNoEquivalent(ComicMediaType type) {
        assertThat(TenraiComicMapper.toTenraiType(type)).isEmpty();
    }
}
