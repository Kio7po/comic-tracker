package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.List;
import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntrySortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;

public interface ComicReadingEntryRepository {
    Optional<ComicReadingEntry> findById(Long id);
    Optional<ComicReadingEntry> findByComicAndSourceAndUrl(Comic comic, ComicReadingSource source, String url);
    List<ComicReadingEntry> findByComic(Comic comic);
    List<ComicReadingEntry> findByComicAndStatus(Comic comic, ComicReadingEntryStatus status);
    List<ComicReadingEntry> findBySourceAndStatus(ComicReadingSource source, ComicReadingEntryStatus status);
    Page<ComicReadingEntry> findByStatusIn(List<ComicReadingEntryStatus> statuses, ComicReadingEntrySortField sortBy,
            SortDirection direction, int limit, int offset);
    ComicReadingEntry save(ComicReadingEntry entry);
}
