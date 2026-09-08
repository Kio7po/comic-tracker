package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicMetadataEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicMetadataSource;

public interface ComicMetadataEntryRepository {
    Optional<ComicMetadataEntry> findBySourceAndExternalId(ComicMetadataSource source, String externalId);
    Optional<ComicMetadataEntry> findByComicAndSource(Comic comic, ComicMetadataSource source);
    ComicMetadataEntry save(ComicMetadataEntry entry);
}
