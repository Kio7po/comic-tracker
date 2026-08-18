package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.Slugifier;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.common.UrlNormalizer;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntrySortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotApprovedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingEntryException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingSourceException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;

@ExtendWith(MockitoExtension.class)
class ComicReadingEntryServiceTest {

    private static final Long COMIC_ID = 1L;
    private static final Long SOURCE_ID = 2L;
    private static final Long ENTRY_ID = 3L;
    private static final Long USER_ID = 4L;
    private static final String URL = "https://example.com/berserk";
    private static final String LOCALE = "es-ES";

    @Mock
    private ComicReadingEntryRepository comicReadingEntryRepository;
    @Mock
    private ComicReadingSourceRepository comicReadingSourceRepository;
    @Mock
    private ComicRepository comicRepository;
    @Mock
    private ComicReadingSourceIconResolverRegistry iconResolverRegistry;
    @Mock
    private UserService userService;

    private ComicReadingEntryService comicReadingEntryService;

    @BeforeEach
    void setUp() {
        comicReadingEntryService = new ComicReadingEntryService(comicReadingEntryRepository,
                comicReadingSourceRepository, comicRepository, iconResolverRegistry, userService);
    }

    @Test
    void submitThrowsWhenComicDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(ComicNotFoundException.class);

        verify(comicReadingSourceRepository, never()).findById(any());
    }

    @Test
    void submitThrowsWhenSourceDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(ComicReadingSourceNotFoundException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitThrowsWhenEntryAlreadyExists() {
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();

        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, URL))
                .thenReturn(Optional.of(new ComicReadingEntry()));

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(DuplicateComicReadingEntryException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitPersistsPendingEntryWithContributor() {
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        User contributor = new User();

        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, URL)).thenReturn(Optional.empty());
        when(userService.findById(USER_ID)).thenReturn(contributor);
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID);

        assertThat(result.getComic()).isSameAs(comic);
        assertThat(result.getSource()).isSameAs(source);
        assertThat(result.getUrl()).isEqualTo(URL);
        assertThat(result.getLocale()).isEqualTo(LOCALE);
        assertThat(result.getContributedBy()).isSameAs(contributor);
        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.PENDING);
    }

    @Test
    void submitWithNewSourceThrowsWhenComicDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.submitWithNewSource(COMIC_ID, "MangaDex",
                "https://mangadex.org", URL, LOCALE, USER_ID)).isInstanceOf(ComicNotFoundException.class);
    }

    @Test
    void submitWithNewSourceThrowsWhenSourceUrlAlreadyRegistered() {
        String sourceUrl = "https://mangadex.org/title/123/chapter-5";
        ComicReadingSource existing = new ComicReadingSource();
        existing.setId(SOURCE_ID);

        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findByUrl(UrlNormalizer.normalizeOrigin(sourceUrl)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> comicReadingEntryService.submitWithNewSource(COMIC_ID, "MangaDex", sourceUrl, URL,
                LOCALE, USER_ID)).isInstanceOf(DuplicateComicReadingSourceException.class);

        verify(comicReadingSourceRepository, never()).save(any());
        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitWithNewSourceCreatesSourceWithURLCollapsedToOriginAndEntry() {
        String sourceUrl = "https://mangadex.org/title/123/chapter-5";
        String iconUrl = "https://mangadex.org/icon.png";
        Comic comic = new Comic();
        User contributor = new User();

        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(userService.findById(USER_ID)).thenReturn(contributor);
        when(comicReadingSourceRepository.findByUrl(UrlNormalizer.normalizeOrigin(sourceUrl)))
                .thenReturn(Optional.empty());
        when(comicReadingSourceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(iconResolverRegistry.resolveIconUrl(any())).thenReturn(Optional.of(iconUrl));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(eq(comic), any(), eq(URL)))
                .thenReturn(Optional.empty());
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submitWithNewSource(COMIC_ID, "MangaDex", sourceUrl, URL,
                LOCALE, USER_ID);

        ComicReadingSource source = result.getSource();
        assertThat(source.getUrl()).isEqualTo(UrlNormalizer.normalizeOrigin(sourceUrl));
        assertThat(source.getSlug()).isEqualTo(Slugifier.slugify("MangaDex"));
        assertThat(source.getName()).isEqualTo("MangaDex");
        assertThat(source.getIconUrl()).isEqualTo(iconUrl);
        assertThat(source.getContributedBy()).isSameAs(contributor);
        assertThat(source.getStatus()).isEqualTo(ComicReadingSourceStatus.PENDING);
        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.PENDING);
    }

    @Test
    void findByComicThrowsWhenComicDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.findByComic(COMIC_ID, null))
                .isInstanceOf(ComicNotFoundException.class);
    }

    @Test
    void findByComicReturnsEveryEntryWhenNoStatusFilterGiven() {
        Comic comic = new Comic();
        List<ComicReadingEntry> entries = List.of(new ComicReadingEntry(), new ComicReadingEntry());
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(entries);

        assertThat(comicReadingEntryService.findByComic(COMIC_ID, null)).isSameAs(entries);

        verify(comicReadingEntryRepository, never()).findByComicAndStatus(any(), any());
    }

    @Test
    void findByComicReturnsOnlyEntriesMatchingTheGivenStatus() {
        Comic comic = new Comic();
        List<ComicReadingEntry> pendingEntries = List.of(new ComicReadingEntry());
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComicAndStatus(comic, ComicReadingEntryStatus.PENDING))
                .thenReturn(pendingEntries);

        assertThat(comicReadingEntryService.findByComic(COMIC_ID, ComicReadingEntryStatus.PENDING))
                .isSameAs(pendingEntries);

        verify(comicReadingEntryRepository, never()).findByComic(any());
    }

    @Test
    void approveThrowsWhenEntryDoesNotExist() {
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.approve(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingEntryNotFoundException.class);
    }

    @Test
    void approveThrowsWhenEntryWasAlreadyReviewed() {
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> comicReadingEntryService.approve(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingEntryAlreadyReviewedException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void approveThrowsWhenSourceIsNotApproved() {
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        source.setStatus(ComicReadingSourceStatus.PENDING);
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setSource(source);
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> comicReadingEntryService.approve(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingSourceNotApprovedException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void approveSetsStatusAndReviewer() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.APPROVED);
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setSource(source);
        User reviewer = new User();
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));
        when(userService.findById(USER_ID)).thenReturn(reviewer);
        when(comicReadingEntryRepository.save(entry)).thenReturn(entry);

        ComicReadingEntry result = comicReadingEntryService.approve(ENTRY_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.APPROVED);
        assertThat(result.getReviewedBy()).isSameAs(reviewer);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void rejectThrowsWhenEntryDoesNotExist() {
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.reject(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingEntryNotFoundException.class);
    }

    @Test
    void rejectThrowsWhenEntryWasAlreadyReviewed() {
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> comicReadingEntryService.reject(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingEntryAlreadyReviewedException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void rejectDoesNotThrowWhenSourceIsNotApproved() {
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        source.setStatus(ComicReadingSourceStatus.PENDING);
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setSource(source);
        User reviewer = new User();
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));
        when(userService.findById(USER_ID)).thenReturn(reviewer);
        when(comicReadingEntryRepository.save(entry)).thenReturn(entry);

        ComicReadingEntry result = comicReadingEntryService.reject(ENTRY_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.REJECTED);
    }

    @Test
    void rejectSetsStatusAndReviewer() {
        ComicReadingEntry entry = new ComicReadingEntry();
        User reviewer = new User();
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));
        when(userService.findById(USER_ID)).thenReturn(reviewer);
        when(comicReadingEntryRepository.save(entry)).thenReturn(entry);

        ComicReadingEntry result = comicReadingEntryService.reject(ENTRY_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.REJECTED);
        assertThat(result.getReviewedBy()).isSameAs(reviewer);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void findByStatusInDelegatesToRepository() {
        List<ComicReadingEntryStatus> statuses = List.of(ComicReadingEntryStatus.PENDING);
        Page<ComicReadingEntry> page = new Page<>(List.of(new ComicReadingEntry()), false, 1);
        when(comicReadingEntryRepository.findByStatusIn(statuses, ComicReadingEntrySortField.CREATED_AT,
                SortDirection.ASC, 20, 0)).thenReturn(page);

        Page<ComicReadingEntry> result = comicReadingEntryService.findByStatusIn(statuses,
                ComicReadingEntrySortField.CREATED_AT, SortDirection.ASC, 20, 0);

        assertThat(result).isSameAs(page);
        verify(comicReadingEntryRepository).findByStatusIn(statuses, ComicReadingEntrySortField.CREATED_AT,
                SortDirection.ASC, 20, 0);
    }

}
