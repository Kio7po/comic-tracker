package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;

public interface ComicReadingEntryRepository {
    Optional<ComicReadingEntry> findByComicAndSourceAndUrl(Comic comic, ComicReadingSource source, String url);
    ComicReadingEntry save(ComicReadingEntry entry);
}
