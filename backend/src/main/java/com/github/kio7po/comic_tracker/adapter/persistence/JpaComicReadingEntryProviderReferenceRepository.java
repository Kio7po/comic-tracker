package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntryProviderReference;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryProviderReferenceRepository;

public interface JpaComicReadingEntryProviderReferenceRepository
        extends JpaRepository<ComicReadingEntryProviderReference, Long>,
        ComicReadingEntryProviderReferenceRepository {
}
