package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSourceProviderReference;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceProviderReferenceRepository;

public interface JpaComicReadingSourceProviderReferenceRepository
        extends JpaRepository<ComicReadingSourceProviderReference, Long>,
        ComicReadingSourceProviderReferenceRepository {
}
