package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingProviderFailedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotApprovedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryTarget;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;

@ExtendWith(MockitoExtension.class)
class ComicReadingSourceServiceTest {

    private static final Long SOURCE_ID = 1L;
    private static final Long USER_ID = 2L;

    @Mock
    private ComicReadingSourceRepository comicReadingSourceRepository;
    @Mock
    private ComicReadingEntryRepository comicReadingEntryRepository;
    @Mock
    private UserService userService;
    @Mock
    private ComicReadingProviderRegistry readingProviderRegistry;

    private ComicReadingSourceService comicReadingSourceService;

    @BeforeEach
    void setUp() {
        comicReadingSourceService = new ComicReadingSourceService(comicReadingSourceRepository,
                comicReadingEntryRepository, userService, readingProviderRegistry);
    }

    @Test
    void searchReturnsTheListingOfTheRegistryForAnApprovedSource() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.APPROVED);
        ComicReadingSearchListing listing = new ComicReadingSearchListing("suwayomi",
                List.of(new ComicReadingSearchResult("7", "Berserk", null)));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.search(source, "berserk")).thenReturn(listing);

        assertThat(comicReadingSourceService.search(SOURCE_ID, "berserk")).isSameAs(listing);
    }

    @Test
    void searchThrowsWhenTheSourceDoesNotExist() {
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingSourceService.search(SOURCE_ID, "berserk"))
                .isInstanceOf(ComicReadingSourceNotFoundException.class);
        verify(readingProviderRegistry, never()).search(any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = ComicReadingSourceStatus.class, names = "APPROVED", mode = EnumSource.Mode.EXCLUDE)
    void searchThrowsWhenTheSourceIsNotApproved(ComicReadingSourceStatus status) {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(status);
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> comicReadingSourceService.search(SOURCE_ID, "berserk"))
                .isInstanceOf(ComicReadingSourceNotApprovedException.class);
        verify(readingProviderRegistry, never()).search(any(), any());
    }

    @Test
    void fetchSearchResultDetailsReturnsTheDetailsTheProviderGives() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.APPROVED);
        ComicReadingEntryDetails details = new ComicReadingEntryDetails("42", "https://example.com/berserk",
                "Berserk", 374, null, null, null, null);
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.fetchFromProvider(source, "suwayomi", new ComicReadingEntryTarget(null, "42")))
                .thenReturn(new FetchOutcome.Found(new FetchedEntry("suwayomi", details)));

        assertThat(comicReadingSourceService.fetchSearchResultDetails(SOURCE_ID, "suwayomi", "42"))
                .isSameAs(details);
    }

    @Test
    void fetchSearchResultDetailsThrowsWhenTheSourceDoesNotExist() {
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingSourceService.fetchSearchResultDetails(SOURCE_ID, "suwayomi", "42"))
                .isInstanceOf(ComicReadingSourceNotFoundException.class);
        verify(readingProviderRegistry, never()).fetchFromProvider(any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = ComicReadingSourceStatus.class, names = "APPROVED", mode = EnumSource.Mode.EXCLUDE)
    void fetchSearchResultDetailsThrowsWhenTheSourceIsNotApproved(ComicReadingSourceStatus status) {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(status);
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> comicReadingSourceService.fetchSearchResultDetails(SOURCE_ID, "suwayomi", "42"))
                .isInstanceOf(ComicReadingSourceNotApprovedException.class);
        verify(readingProviderRegistry, never()).fetchFromProvider(any(), any(), any());
    }

    @Test
    void fetchSearchResultDetailsThrowsWhenTheProviderFails() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.APPROVED);
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.fetchFromProvider(source, "suwayomi", new ComicReadingEntryTarget(null, "42")))
                .thenReturn(new FetchOutcome.Failed(new RuntimeException("site is down")));

        assertThatThrownBy(() -> comicReadingSourceService.fetchSearchResultDetails(SOURCE_ID, "suwayomi", "42"))
                .isInstanceOf(ComicReadingProviderFailedException.class);
    }

    @Test
    void findByStatusInDelegatesToRepository() {
        ComicReadingSource source1 = new ComicReadingSource();
        ComicReadingSource source2 = new ComicReadingSource();
        List<ComicReadingSourceStatus> statuses = List.of(ComicReadingSourceStatus.PENDING);

        when(comicReadingSourceRepository.findByStatusIn(
            statuses, ComicReadingSourceSortField.NAME, SortDirection.ASC
        )).thenReturn(List.of(source1, source2));

        List<ComicReadingSource> result = comicReadingSourceService.findByStatusIn(
            statuses, ComicReadingSourceSortField.NAME, SortDirection.ASC
        );

        assertThat(result).containsExactly(source1, source2);

        verify(comicReadingSourceRepository)
            .findByStatusIn(statuses, ComicReadingSourceSortField.NAME, SortDirection.ASC);
    }

    @Test
    void approveThrowsWhenSourceDoesNotExist() {
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingSourceService.approve(SOURCE_ID, USER_ID))
                .isInstanceOf(ComicReadingSourceNotFoundException.class);
    }

    @Test
    void approveThrowsWhenSourceWasAlreadyReviewed() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.APPROVED);
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> comicReadingSourceService.approve(SOURCE_ID, USER_ID))
                .isInstanceOf(ComicReadingSourceAlreadyReviewedException.class);

        verify(comicReadingSourceRepository, never()).save(any());
    }

    @Test
    void approveSetsStatusAndReviewer() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.PENDING);
        User reviewer = new User();
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(userService.findById(USER_ID)).thenReturn(reviewer);
        when(comicReadingSourceRepository.save(source)).thenReturn(source);

        ComicReadingSource result = comicReadingSourceService.approve(SOURCE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(ComicReadingSourceStatus.APPROVED);
        assertThat(result.getReviewedBy()).isSameAs(reviewer);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void rejectThrowsWhenSourceDoesNotExist() {
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingSourceService.reject(SOURCE_ID, USER_ID))
                .isInstanceOf(ComicReadingSourceNotFoundException.class);
    }

    @Test
    void rejectThrowsWhenSourceWasAlreadyReviewed() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.REJECTED);
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> comicReadingSourceService.reject(SOURCE_ID, USER_ID))
                .isInstanceOf(ComicReadingSourceAlreadyReviewedException.class);

        verify(comicReadingSourceRepository, never()).save(any());
    }

    @Test
    void rejectSetsStatusAndReviewer() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.PENDING);
        User reviewer = new User();
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(userService.findById(USER_ID)).thenReturn(reviewer);
        when(comicReadingSourceRepository.save(source)).thenReturn(source);

        ComicReadingSource result = comicReadingSourceService.reject(SOURCE_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(ComicReadingSourceStatus.REJECTED);
        assertThat(result.getReviewedBy()).isSameAs(reviewer);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void rejectCascadesToStillPendingEntries() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.PENDING);
        User reviewer = new User();
        ComicReadingEntry pendingEntry = new ComicReadingEntry();
        pendingEntry.setStatus(ComicReadingEntryStatus.PENDING);
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(userService.findById(USER_ID)).thenReturn(reviewer);
        when(comicReadingSourceRepository.save(source)).thenReturn(source);
        when(comicReadingEntryRepository.findBySourceAndStatus(source, ComicReadingEntryStatus.PENDING))
                .thenReturn(List.of(pendingEntry));

        comicReadingSourceService.reject(SOURCE_ID, USER_ID);

        assertThat(pendingEntry.getStatus()).isEqualTo(ComicReadingEntryStatus.REJECTED);
        assertThat(pendingEntry.getReviewedBy()).isSameAs(reviewer);
        assertThat(pendingEntry.getReviewedAt()).isNotNull();
        verify(comicReadingEntryRepository).save(pendingEntry);
    }

}