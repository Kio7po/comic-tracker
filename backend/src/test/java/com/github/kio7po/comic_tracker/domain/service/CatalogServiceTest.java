package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.Slugifier;
import com.github.kio7po.comic_tracker.domain.entities.Author;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicMetadataEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicMetadataSource;
import com.github.kio7po.comic_tracker.domain.entities.Genre;
import com.github.kio7po.comic_tracker.domain.entities.Tag;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicMetadataSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.UnsupportedMetadataSourceException;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataProvider;
import com.github.kio7po.comic_tracker.domain.port.metadata.ComicMetadataResult;
import com.github.kio7po.comic_tracker.domain.port.persistence.AuthorRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicMetadataEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicMetadataSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.GenreRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.TagRepository;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    private static final String SOURCE_SLUG = "myanimelist";
    private static final String EXTERNAL_ID = "152";
    private static final String SLUG = "berserk";
    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration RETRY_BACKOFF = Duration.ofMinutes(15);

    @Mock
    private ComicMetadataProvider metadataProvider;
    @Mock
    private ComicRepository comicRepository;
    @Mock
    private ComicMetadataEntryRepository comicMetadataEntryRepository;
    @Mock
    private ComicMetadataSourceRepository comicMetadataSourceRepository;
    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ComicService comicService;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(metadataProvider, comicRepository, comicMetadataEntryRepository,
                comicMetadataSourceRepository, authorRepository, genreRepository, tagRepository, comicService, TTL,
                RETRY_BACKOFF);
    }

    @Test
    void searchDelegatesToMetadataProvider() {
        Page<ComicMetadataResult> expected = new Page<>(List.of(), false, null);
        when(metadataProvider.search("berserk", 20, 0, null, null, null)).thenReturn(expected);

        Page<ComicMetadataResult> result = catalogService.search("berserk", 20, 0, null, null, null);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void importComicThrowsWhenSourceSlugIsNotTheActiveProvider() {
        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);

        assertThatThrownBy(() -> catalogService.importComic("anilist", EXTERNAL_ID))
                .isInstanceOf(UnsupportedMetadataSourceException.class);

        verify(comicMetadataSourceRepository, never()).findBySlug(any());
    }

    @Test
    void importComicThrowsWhenSourceIsNotSeeded() {
        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalogService.importComic(SOURCE_SLUG, EXTERNAL_ID))
                .isInstanceOf(ComicMetadataSourceNotFoundException.class);
    }

    @Test
    void importComicReturnsExistingComicUnchangedWhenEntryIsFresh() {
        ComicMetadataSource source = new ComicMetadataSource();
        Comic existingComic = new Comic();
        ComicMetadataEntry entry = new ComicMetadataEntry();
        entry.setComic(existingComic);
        entry.setLastFetchedAt(Instant.now());

        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findBySourceAndExternalId(source, EXTERNAL_ID))
                .thenReturn(Optional.of(entry));

        Optional<Comic> result = catalogService.importComic(SOURCE_SLUG, EXTERNAL_ID);

        assertThat(result).contains(existingComic);
        verify(metadataProvider, never()).fetch(any());
        verify(comicRepository, never()).save(any());
    }

    @Test
    void importComicRefreshesTheExistingComicWhenEntryIsStale() {
        ComicMetadataSource source = new ComicMetadataSource();
        Comic existingComic = new Comic();
        existingComic.setTitle("Old title");
        ComicMetadataEntry entry = new ComicMetadataEntry();
        entry.setComic(existingComic);
        entry.setExternalId(EXTERNAL_ID);
        entry.setLastFetchedAt(Instant.now().minus(TTL).minusSeconds(1));

        Comic fetchedComic = new Comic();
        fetchedComic.setTitle("New title");
        fetchedComic.setAuthors(Set.of());
        fetchedComic.setGenres(Set.of());
        fetchedComic.setTags(Set.of());
        ComicMetadataResult fetchResult = new ComicMetadataResult(SOURCE_SLUG, EXTERNAL_ID, fetchedComic);

        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findBySourceAndExternalId(source, EXTERNAL_ID))
                .thenReturn(Optional.of(entry));
        when(metadataProvider.fetch(EXTERNAL_ID)).thenReturn(Optional.of(fetchResult));
        when(comicRepository.save(existingComic)).thenReturn(existingComic);

        Optional<Comic> result = catalogService.importComic(SOURCE_SLUG, EXTERNAL_ID);

        assertThat(result).contains(existingComic);
        assertThat(existingComic.getTitle()).isEqualTo("New title");
        verify(comicMetadataEntryRepository).save(entry);
        assertThat(entry.getLastFetchedAt()).isAfter(Instant.now().minusSeconds(2));
    }

    @Test
    void importComicReturnsEmptyWhenProviderCannotFetchTheComic() {
        ComicMetadataSource source = new ComicMetadataSource();

        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findBySourceAndExternalId(source, EXTERNAL_ID))
                .thenReturn(Optional.empty());
        when(metadataProvider.fetch(EXTERNAL_ID)).thenReturn(Optional.empty());

        Optional<Comic> result = catalogService.importComic(SOURCE_SLUG, EXTERNAL_ID);

        assertThat(result).isEmpty();
        verify(comicRepository, never()).save(any());
    }

    @Test
    void importComicPersistsNewComicReusingExistingAuthorsGenresAndTags() {
        ComicMetadataSource source = new ComicMetadataSource();

        Author fetchedAuthor = new Author();
        fetchedAuthor.setName("Kentaro Miura");
        Author existingAuthor = new Author();
        existingAuthor.setName("Kentaro Miura");
        existingAuthor.setId(1L);

        Genre fetchedGenre = new Genre();
        fetchedGenre.setName("Action");

        Tag fetchedTag = new Tag();
        fetchedTag.setName("Seinen");

        Comic fetchedComic = new Comic();
        fetchedComic.setTitle("Berserk");
        fetchedComic.setAuthors(Set.of(fetchedAuthor));
        fetchedComic.setGenres(Set.of(fetchedGenre));
        fetchedComic.setTags(Set.of(fetchedTag));

        ComicMetadataResult fetchResult = new ComicMetadataResult(SOURCE_SLUG, EXTERNAL_ID, fetchedComic);

        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findBySourceAndExternalId(source, EXTERNAL_ID))
                .thenReturn(Optional.empty());
        when(metadataProvider.fetch(EXTERNAL_ID)).thenReturn(Optional.of(fetchResult));

        // Author already exists by name -> reused instead of inserted again.
        when(authorRepository.findByName("Kentaro Miura")).thenReturn(Optional.of(existingAuthor));
        // Genre/Tag are new -> created.
        when(genreRepository.findByName("Action")).thenReturn(Optional.empty());
        when(genreRepository.save(fetchedGenre)).thenReturn(fetchedGenre);
        when(tagRepository.findByName("Seinen")).thenReturn(Optional.empty());
        when(tagRepository.save(fetchedTag)).thenReturn(fetchedTag);

        when(comicRepository.findBySlug("berserk")).thenReturn(Optional.empty());
        when(comicRepository.save(fetchedComic)).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Comic> result = catalogService.importComic(SOURCE_SLUG, EXTERNAL_ID);

        assertThat(result).isPresent();
        Comic savedComic = result.get();
        assertThat(savedComic.getSlug()).isEqualTo("berserk");
        assertThat(savedComic.getAuthors()).containsExactly(existingAuthor);
        assertThat(savedComic.getGenres()).containsExactly(fetchedGenre);
        assertThat(savedComic.getTags()).containsExactly(fetchedTag);

        verify(authorRepository, never()).save(any());
        verify(comicMetadataEntryRepository, times(1)).save(argThat((ComicMetadataEntry entry) -> entry
                .getExternalId().equals(EXTERNAL_ID) && entry.getComic() == savedComic && entry.getSource() == source
                && entry.getLastFetchedAt() != null));
    }

    @ParameterizedTest
    @CsvSource({ "152", "9999" })
    void importComicAppendsExternalIdToSlugOnCollision(String externalId) {
        ComicMetadataSource source = new ComicMetadataSource();

        Comic fetchedComic = new Comic();
        fetchedComic.setTitle("Berserk");
        fetchedComic.setAuthors(Set.of());
        fetchedComic.setGenres(Set.of());
        fetchedComic.setTags(Set.of());

        ComicMetadataResult fetchResult = new ComicMetadataResult(SOURCE_SLUG, externalId, fetchedComic);

        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findBySourceAndExternalId(source, externalId)).thenReturn(Optional.empty());
        when(metadataProvider.fetch(externalId)).thenReturn(Optional.of(fetchResult));
        // A different comic already took the plain "berserk" slug -> forces the collision branch.
        when(comicRepository.findBySlug("berserk")).thenReturn(Optional.of(new Comic()));
        when(comicRepository.save(fetchedComic)).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Comic> result = catalogService.importComic(SOURCE_SLUG, externalId);

        assertThat(result).isPresent();
        assertThat(result.get().getSlug()).isEqualTo(Slugifier.slugify("Berserk") + "-" + externalId);
    }

    @Test
    void getDetailReturnsEmptyWhenComicNotFound() {
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.empty());

        Optional<Comic> result = catalogService.getDetail(SLUG);

        assertThat(result).isEmpty();
        verify(comicMetadataSourceRepository, never()).findBySlug(any());
    }

    @Test
    void getDetailReturnsComicUnchangedWhenActiveSourceIsNotSeeded() {
        Comic comic = new Comic();
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic));
        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.empty());

        Optional<Comic> result = catalogService.getDetail(SLUG);

        assertThat(result).contains(comic);
        verify(metadataProvider, never()).fetch(any());
    }

    @Test
    void getDetailReturnsComicUnchangedWhenNoEntryExistsForTheActiveSource() {
        ComicMetadataSource source = new ComicMetadataSource();
        Comic comic = new Comic();
        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic));
        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findByComicAndSource(comic, source)).thenReturn(Optional.empty());

        Optional<Comic> result = catalogService.getDetail(SLUG);

        assertThat(result).contains(comic);
        verify(metadataProvider, never()).fetch(any());
    }

    @Test
    void getDetailReturnsComicUnchangedWhenEntryIsFresh() {
        ComicMetadataSource source = new ComicMetadataSource();
        Comic comic = new Comic();
        ComicMetadataEntry entry = new ComicMetadataEntry();
        entry.setLastFetchedAt(Instant.now());

        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic));
        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findByComicAndSource(comic, source)).thenReturn(Optional.of(entry));

        Optional<Comic> result = catalogService.getDetail(SLUG);

        assertThat(result).contains(comic);
        verify(metadataProvider, never()).fetch(any());
        verify(comicRepository, never()).save(any());
    }

    @Test
    void getDetailRefreshesTheComicWhenEntryIsStale() {
        ComicMetadataSource source = new ComicMetadataSource();
        Comic comic = new Comic();
        comic.setTitle("Old title");
        ComicMetadataEntry entry = new ComicMetadataEntry();
        entry.setExternalId(EXTERNAL_ID);
        entry.setLastFetchedAt(Instant.now().minus(TTL).minusSeconds(1));

        Comic fetchedComic = new Comic();
        fetchedComic.setTitle("New title");
        fetchedComic.setAuthors(Set.of());
        fetchedComic.setGenres(Set.of());
        fetchedComic.setTags(Set.of());
        ComicMetadataResult fetchResult = new ComicMetadataResult(SOURCE_SLUG, EXTERNAL_ID, fetchedComic);

        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic));
        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findByComicAndSource(comic, source)).thenReturn(Optional.of(entry));
        when(metadataProvider.fetch(EXTERNAL_ID)).thenReturn(Optional.of(fetchResult));
        when(comicRepository.save(comic)).thenReturn(comic);

        Optional<Comic> result = catalogService.getDetail(SLUG);

        assertThat(result).contains(comic);
        assertThat(comic.getTitle()).isEqualTo("New title");
        verify(comicMetadataEntryRepository).save(entry);
        assertThat(entry.getLastFetchedAt()).isAfter(Instant.now().minusSeconds(2));
    }

    @Test
    void getDetailLeavesTheComicUnchangedButBumpsLastFetchedAtWhenTheFetchFindsNothing() {
        ComicMetadataSource source = new ComicMetadataSource();
        Comic comic = new Comic();
        comic.setTitle("Old title");
        ComicMetadataEntry entry = new ComicMetadataEntry();
        entry.setExternalId(EXTERNAL_ID);
        entry.setLastFetchedAt(Instant.now().minus(TTL).minusSeconds(1));

        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic));
        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findByComicAndSource(comic, source)).thenReturn(Optional.of(entry));
        when(metadataProvider.fetch(EXTERNAL_ID)).thenReturn(Optional.empty());
        when(comicRepository.save(comic)).thenReturn(comic);

        Optional<Comic> result = catalogService.getDetail(SLUG);

        assertThat(result).contains(comic);
        assertThat(comic.getTitle()).isEqualTo("Old title");
        assertThat(entry.getLastFetchedAt()).isAfter(Instant.now().minusSeconds(2));
    }

    @Test
    void getDetailAppliesAShortBackoffInsteadOfTheFullTtlWhenTheRefreshFails() {
        ComicMetadataSource source = new ComicMetadataSource();
        Comic comic = new Comic();
        comic.setTitle("Old title");
        ComicMetadataEntry entry = new ComicMetadataEntry();
        entry.setExternalId(EXTERNAL_ID);
        entry.setLastFetchedAt(Instant.now().minus(TTL).minusSeconds(1));

        when(comicService.findBySlug(SLUG)).thenReturn(Optional.of(comic));
        when(metadataProvider.getSourceSlug()).thenReturn(SOURCE_SLUG);
        when(comicMetadataSourceRepository.findBySlug(SOURCE_SLUG)).thenReturn(Optional.of(source));
        when(comicMetadataEntryRepository.findByComicAndSource(comic, source)).thenReturn(Optional.of(entry));
        when(metadataProvider.fetch(EXTERNAL_ID)).thenThrow(new RuntimeException("boom"));
        when(comicRepository.save(comic)).thenReturn(comic);

        Instant before = Instant.now();
        Optional<Comic> result = catalogService.getDetail(SLUG);
        Instant after = Instant.now();

        assertThat(result).contains(comic);
        assertThat(comic.getTitle()).isEqualTo("Old title");
        // Stale again after RETRY_BACKOFF, not after a full TTL cycle.
        assertThat(entry.getLastFetchedAt().plus(TTL))
                .isBetween(before.plus(RETRY_BACKOFF).minusSeconds(1), after.plus(RETRY_BACKOFF).plusSeconds(1));
    }

}
