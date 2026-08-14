package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.List;
import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;

public interface ComicReadingEntryRepository {
    Optional<ComicReadingEntry> findById(Long id);
    Optional<ComicReadingEntry> findByComicAndSourceAndUrl(Comic comic, ComicReadingSource source, String url);
    List<ComicReadingEntry> findByComic(Comic comic);
    List<ComicReadingEntry> findByComicAndStatus(Comic comic, ComicReadingEntryStatus status);
    ComicReadingEntry save(ComicReadingEntry entry);
}
