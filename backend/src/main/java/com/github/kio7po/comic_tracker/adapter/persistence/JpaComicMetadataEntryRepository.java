package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.ComicMetadataEntry;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicMetadataEntryRepository;

public interface JpaComicMetadataEntryRepository
        extends JpaRepository<ComicMetadataEntry, Long>, ComicMetadataEntryRepository {
}
