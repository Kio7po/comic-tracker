package com.github.kio7po.comic_tracker.adapter.metadata;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.kio7po.comic_tracker.domain.entities.Author;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.Genre;
import com.github.kio7po.comic_tracker.domain.entities.Tag;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicMediaType;
import com.github.kio7po.comic_tracker.domain.enums.NsfwRating;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;

final class TenraiComicMapper {

    private TenraiComicMapper() {
    }

    static ComicMetadataResult toResult(TenraiMangaDto dto, String sourceSlug) {
        return new ComicMetadataResult(sourceSlug, String.valueOf(dto.malId()), toComic(dto));
    }

    private static Comic toComic(TenraiMangaDto dto) {
        Comic comic = new Comic();
        comic.setTitle(dto.title());
        comic.setAlternativeTitles(alternativeTitles(dto));
        comic.setSynopsis(dto.synopsis());
        comic.setCoverUrl(coverUrl(dto));
        comic.setStartDate(toLocalDate(dto.published() == null ? null : dto.published().from()));
        comic.setEndDate(toLocalDate(dto.published() == null ? null : dto.published().to()));
        comic.setMediaType(toMediaType(dto.type()));
        comic.setStatus(toStatus(dto.status()));
        comic.setChapters(dto.chapters());
        comic.setNsfw(toNsfwRating(dto.explicitGenres()));
        comic.setAuthors(toAuthors(dto.authors()));
        comic.setGenres(toGenres(dto.genres(), dto.explicitGenres()));
        comic.setTags(toTags(dto.themes(), dto.demographics()));
        return comic;
    }

    private static Set<String> alternativeTitles(TenraiMangaDto dto) {
        Set<String> titles = new HashSet<>();
        addIfPresent(titles, dto.titleEnglish());
        addIfPresent(titles, dto.titleJapanese());
        if (dto.titleSynonyms() != null) {
            dto.titleSynonyms().forEach(title -> addIfPresent(titles, title));
        }
        return titles;
    }

    private static void addIfPresent(Set<String> titles, String title) {
        if (title != null && !title.isBlank()) {
            titles.add(title);
        }
    }

    private static String coverUrl(TenraiMangaDto dto) {
        // podría cambiar
        if (dto.images() == null || dto.images().jpg() == null) {
            return null;
        }
        return dto.images().jpg().largeImageUrl();
    }

    private static LocalDate toLocalDate(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    private static Set<Author> toAuthors(List<TenraiNamedResourceDto> authors) {
        if (authors == null)
            return Set.of();

        return authors.stream()
            .map(resource -> {
                Author author = new Author();
                author.setName(resource.name());
                return author;
            }).collect(Collectors.toCollection(HashSet::new));
    }

    private static <T, R> Set<R> concatAndMap(List<T> list1, List<T> list2, Function<T, R> mapper) {
        return Stream.concat(
            list1 == null ? Stream.empty() : list1.stream(),
            list2 == null ? Stream.empty() : list2.stream())
        .map(mapper)
        .collect(Collectors.toCollection(HashSet::new));
    }

    private static Set<Genre> toGenres(List<TenraiNamedResourceDto> genres, List<TenraiNamedResourceDto> explicitGenres) {
        return concatAndMap(genres, explicitGenres, resource -> {
            Genre genre = new Genre();
            genre.setName(resource.name());
            return genre;
        });
    }

    private static Set<Tag> toTags(List<TenraiNamedResourceDto> themes, List<TenraiNamedResourceDto> demographics) {
        return concatAndMap(themes, demographics, resource -> {
            Tag tag = new Tag();
            tag.setName(resource.name());
            return tag;
        });
    }

    private static ComicMediaType toMediaType(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "Manga" -> ComicMediaType.MANGA;
            case "Novel", "Light Novel" -> ComicMediaType.NOVEL;
            case "One-shot" -> ComicMediaType.ONE_SHOT;
            case "Doujinshi" -> ComicMediaType.DOUJINSHI;
            case "Manhwa" -> ComicMediaType.MANHWA;
            case "Manhua" -> ComicMediaType.MANHUA;
            default -> ComicMediaType.OTHER;
        };
    }

    private static ComicStatus toStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case "Publishing" -> ComicStatus.ONGOING;
            case "Finished" -> ComicStatus.COMPLETED;
            case "On Hiatus" -> ComicStatus.HIATUS;
            case "Discontinued" -> ComicStatus.CANCELLED;
            default -> ComicStatus.OTHER;
        };
    }

    private static NsfwRating toNsfwRating(List<TenraiNamedResourceDto> explicitGenres) {
        // se podría distinguir de manera más fina
        return explicitGenres != null && !explicitGenres.isEmpty() ? NsfwRating.EXPLICIT : NsfwRating.NONE;
    }

    static Optional<String> toTenraiStatus(ComicStatus status) {
        if (status == null) {
            return Optional.empty();
        }
        return switch (status) {
            case ONGOING -> Optional.of("publishing");
            case COMPLETED -> Optional.of("complete");
            case HIATUS -> Optional.of("hiatus");
            case CANCELLED -> Optional.of("discontinued");
            case OTHER -> Optional.empty();
        };
    }

    static Optional<String> toTenraiType(ComicMediaType type) {
        if (type == null) {
            return Optional.empty();
        }
        return switch (type) {
            case MANGA -> Optional.of("manga");
            case NOVEL -> Optional.of("novel");
            case ONE_SHOT -> Optional.of("oneshot");
            case DOUJINSHI -> Optional.of("doujin");
            case MANHWA -> Optional.of("manhwa");
            case MANHUA -> Optional.of("manhua");
            case WEBTOON, COMIC, OTHER -> Optional.empty();
        };
    }
}
