package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;

public interface ComicReadingSourceRepository {
    Optional<ComicReadingSource> findById(Long id);
    Optional<ComicReadingSource> findBySlug(String slug);
}
