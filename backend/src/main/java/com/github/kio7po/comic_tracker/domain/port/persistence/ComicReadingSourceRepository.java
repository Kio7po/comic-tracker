package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.List;
import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;

public interface ComicReadingSourceRepository {
    Optional<ComicReadingSource> findById(Long id);
    Optional<ComicReadingSource> findBySlug(String slug);
    Optional<ComicReadingSource> findByUrl(String url);
    List<ComicReadingSource> findByStatusNotOrderByNameAsc(ComicReadingSourceStatus excludedStatus);
    ComicReadingSource save(ComicReadingSource source);
}
