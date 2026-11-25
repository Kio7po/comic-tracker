package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingCoverUnavailableException;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverImage;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverProvider;

@ExtendWith(MockitoExtension.class)
class ComicReadingCoverServiceTest {

    @Mock
    private ComicReadingCoverProvider suwayomiCoverProvider;
    @Mock
    private ComicReadingCoverProvider otherCoverProvider;

    private ComicReadingCoverService comicReadingCoverService;

    @BeforeEach
    void setUp() {
        lenient().when(suwayomiCoverProvider.key()).thenReturn("suwayomi");
        lenient().when(otherCoverProvider.key()).thenReturn("other");
        comicReadingCoverService = new ComicReadingCoverService(List.of(otherCoverProvider, suwayomiCoverProvider));
    }

    @Test
    void findCoverAsksTheProviderThatHasThatKey() {
        ComicReadingCoverImage image = new ComicReadingCoverImage(new byte[] { 1 }, "image/webp");
        when(suwayomiCoverProvider.fetchCover("731")).thenReturn(Optional.of(image));

        assertThat(comicReadingCoverService.findCover("suwayomi", "731")).containsSame(image);
        verify(otherCoverProvider, never()).fetchCover(any());
    }

    @Test
    void findCoverIsEmptyWhenNoProviderHasThatKey() {
        assertThat(comicReadingCoverService.findCover("unknown", "731")).isEmpty();

        verify(suwayomiCoverProvider, never()).fetchCover(any());
        verify(otherCoverProvider, never()).fetchCover(any());
    }

    @Test
    void findCoverIsEmptyWhenTheProviderHasNoSuchCover() {
        when(suwayomiCoverProvider.fetchCover("731")).thenReturn(Optional.empty());

        assertThat(comicReadingCoverService.findCover("suwayomi", "731")).isEmpty();
    }

    @Test
    void findCoverThrowsKeepingTheCauseWhenTheProviderFails() {
        RuntimeException cause = new IllegalStateException("suwayomi is down");
        when(suwayomiCoverProvider.fetchCover("731")).thenThrow(cause);

        assertThatThrownBy(() -> comicReadingCoverService.findCover("suwayomi", "731"))
                .isInstanceOfSatisfying(ComicReadingCoverUnavailableException.class, exception -> {
                    assertThat(exception.getProviderKey()).isEqualTo("suwayomi");
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }

}
