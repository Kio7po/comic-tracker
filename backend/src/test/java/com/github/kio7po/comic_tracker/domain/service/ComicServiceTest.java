package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;

@ExtendWith(MockitoExtension.class)
class ComicServiceTest {

    private static final String SLUG = "berserk";

    @Mock
    private ComicRepository comicRepository;

    private ComicService comicService;

    @BeforeEach
    void setUp() {
        comicService = new ComicService(comicRepository);
    }

    @Test
    void findBySlugReturnsComicWhenPresent() {
        Comic comic = new Comic();
        comic.setSlug(SLUG);
        when(comicRepository.findBySlug(SLUG)).thenReturn(Optional.of(comic));

        Optional<Comic> result = comicService.findBySlug(SLUG);

        assertThat(result).contains(comic);
    }

    @Test
    void findBySlugReturnsEmptyWhenNotFound() {
        when(comicRepository.findBySlug(SLUG)).thenReturn(Optional.empty());

        Optional<Comic> result = comicService.findBySlug(SLUG);

        assertThat(result).isEmpty();
    }

}
