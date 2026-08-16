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
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;

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

    private ComicReadingSourceService comicReadingSourceService;

    @BeforeEach
    void setUp() {
        comicReadingSourceService = new ComicReadingSourceService(comicReadingSourceRepository,
                comicReadingEntryRepository, userService);
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