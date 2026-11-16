package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.List;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntryProviderReference;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;

public interface ComicReadingEntryProviderReferenceRepository {
    List<ComicReadingEntryProviderReference> findByEntryIn(List<ComicReadingEntry> entries);
    boolean existsByEntryComicAndEntrySourceAndProviderKeyAndExternalId(Comic comic, ComicReadingSource source,
            String providerKey, String externalId);
    ComicReadingEntryProviderReference save(ComicReadingEntryProviderReference reference);
    List<ComicReadingEntryProviderReference> saveAll(List<ComicReadingEntryProviderReference> references);
}
