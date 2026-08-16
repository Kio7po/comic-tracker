package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;

@ExtendWith(MockitoExtension.class)
class ComicReadingSourceServiceTest {

    @Mock
    private ComicReadingSourceRepository comicReadingSourceRepository;

    private ComicReadingSourceService comicReadingSourceService;

    @BeforeEach
    void setUp() {
        comicReadingSourceService = new ComicReadingSourceService(comicReadingSourceRepository);
    }

    @Test
    void findSelectableExcludesOnlyRejectedSources() {
        ComicReadingSource source1 = new ComicReadingSource();
        ComicReadingSource source2 = new ComicReadingSource();

        when(comicReadingSourceRepository.findByStatusNotOrderByNameAsc(
            ComicReadingSourceStatus.REJECTED
        )).thenReturn(List.of(source1, source2));

        List<ComicReadingSource> result = comicReadingSourceService.findSelectable();

        assertThat(result).containsExactly(source1, source2);

        verify(comicReadingSourceRepository)
            .findByStatusNotOrderByNameAsc(ComicReadingSourceStatus.REJECTED);
    }

}