package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.List;
import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSourceProviderReference;

public interface ComicReadingSourceProviderReferenceRepository {
    List<ComicReadingSourceProviderReference> findBySourceIn(List<ComicReadingSource> sources);
    List<ComicReadingSourceProviderReference> findByProviderKey(String providerKey);
    Optional<ComicReadingSourceProviderReference> findByProviderKeyAndExternalId(String providerKey,
            String externalId);
    ComicReadingSourceProviderReference save(ComicReadingSourceProviderReference reference);
}
