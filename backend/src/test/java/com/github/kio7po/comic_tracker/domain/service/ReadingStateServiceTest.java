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

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ReadingState;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ReadingStateStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateNotFoundException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ReadingStateRepository;

@ExtendWith(MockitoExtension.class)
class ReadingStateServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long COMIC_ID = 2L;

    @Mock
    private ReadingStateRepository readingStateRepository;
    @Mock
    private ComicRepository comicRepository;
    @Mock
    private UserService userService;

    private ReadingStateService readingStateService;

    @BeforeEach
    void setUp() {
        readingStateService = new ReadingStateService(readingStateRepository, comicRepository, userService);
    }

    @Test
    void createThrowsWhenComicDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> readingStateService.create(USER_ID, COMIC_ID, ReadingStateStatus.PLAN_TO_READ, 0))
                        .isInstanceOf(ComicNotFoundException.class);

        verify(readingStateRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenReadingStateAlreadyExists() {
        User user = new User();
        Comic comic = new Comic();
        when(userService.findById(USER_ID)).thenReturn(user);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(readingStateRepository.findByUserAndComic(user, comic)).thenReturn(Optional.of(new ReadingState()));

        assertThatThrownBy(
                () -> readingStateService.create(USER_ID, COMIC_ID, ReadingStateStatus.PLAN_TO_READ, 0))
                        .isInstanceOf(ReadingStateAlreadyExistsException.class);

        verify(readingStateRepository, never()).save(any());
    }

    @Test
    void createPersistsReadingStateWithGivenStatusAndChapters() {
        User user = new User();
        Comic comic = new Comic();
        when(userService.findById(USER_ID)).thenReturn(user);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(readingStateRepository.findByUserAndComic(user, comic)).thenReturn(Optional.empty());
        when(readingStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReadingState result = readingStateService.create(USER_ID, COMIC_ID, ReadingStateStatus.READING, 12);

        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getComic()).isSameAs(comic);
        assertThat(result.getStatus()).isEqualTo(ReadingStateStatus.READING);
        assertThat(result.getChapters()).isEqualTo(12);
    }

    @Test
    void findByUserAndComicThrowsWhenComicDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readingStateService.findByUserAndComic(USER_ID, COMIC_ID))
                .isInstanceOf(ComicNotFoundException.class);
    }

    @Test
    void findByUserAndComicReturnsEmptyWhenNoneTracked() {
        User user = new User();
        Comic comic = new Comic();
        when(userService.findById(USER_ID)).thenReturn(user);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(readingStateRepository.findByUserAndComic(user, comic)).thenReturn(Optional.empty());

        assertThat(readingStateService.findByUserAndComic(USER_ID, COMIC_ID)).isEmpty();
    }

    @Test
    void findByUserAndComicReturnsExistingReadingState() {
        User user = new User();
        Comic comic = new Comic();
        ReadingState readingState = new ReadingState();
        when(userService.findById(USER_ID)).thenReturn(user);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(readingStateRepository.findByUserAndComic(user, comic)).thenReturn(Optional.of(readingState));

        assertThat(readingStateService.findByUserAndComic(USER_ID, COMIC_ID)).contains(readingState);
    }

    @Test
    void findByUserReturnsRepositoryResult() {
        User user = new User();
        List<ReadingState> readingStates = List.of(new ReadingState(), new ReadingState());
        when(userService.findById(USER_ID)).thenReturn(user);
        when(readingStateRepository.findByUser(user)).thenReturn(readingStates);

        assertThat(readingStateService.findByUser(USER_ID)).isSameAs(readingStates);
    }

    @Test
    void updateThrowsWhenComicDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> readingStateService.update(USER_ID, COMIC_ID, ReadingStateStatus.COMPLETED, 50))
                        .isInstanceOf(ComicNotFoundException.class);

        verify(readingStateRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenReadingStateDoesNotExist() {
        User user = new User();
        Comic comic = new Comic();
        when(userService.findById(USER_ID)).thenReturn(user);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(readingStateRepository.findByUserAndComic(user, comic)).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> readingStateService.update(USER_ID, COMIC_ID, ReadingStateStatus.COMPLETED, 50))
                        .isInstanceOf(ReadingStateNotFoundException.class);

        verify(readingStateRepository, never()).save(any());
    }

    @Test
    void updateReplacesStatusAndChapters() {
        User user = new User();
        Comic comic = new Comic();
        ReadingState readingState = new ReadingState();
        readingState.setStatus(ReadingStateStatus.READING);
        readingState.setChapters(10);
        when(userService.findById(USER_ID)).thenReturn(user);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(readingStateRepository.findByUserAndComic(user, comic)).thenReturn(Optional.of(readingState));
        when(readingStateRepository.save(readingState)).thenReturn(readingState);

        ReadingState result = readingStateService.update(USER_ID, COMIC_ID, ReadingStateStatus.COMPLETED, 50);

        assertThat(result.getStatus()).isEqualTo(ReadingStateStatus.COMPLETED);
        assertThat(result.getChapters()).isEqualTo(50);
    }

    @Test
    void deleteThrowsWhenReadingStateDoesNotExist() {
        User user = new User();
        Comic comic = new Comic();
        when(userService.findById(USER_ID)).thenReturn(user);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(readingStateRepository.findByUserAndComic(user, comic)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> readingStateService.delete(USER_ID, COMIC_ID))
                .isInstanceOf(ReadingStateNotFoundException.class);

        verify(readingStateRepository, never()).delete(any());
    }

    @Test
    void deleteRemovesExistingReadingState() {
        User user = new User();
        Comic comic = new Comic();
        ReadingState readingState = new ReadingState();
        when(userService.findById(USER_ID)).thenReturn(user);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(readingStateRepository.findByUserAndComic(user, comic)).thenReturn(Optional.of(readingState));

        readingStateService.delete(USER_ID, COMIC_ID);

        verify(readingStateRepository).delete(readingState);
    }

}
