package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;

public interface JpaComicReadingEntryRepository
        extends JpaRepository<ComicReadingEntry, Long>, ComicReadingEntryRepository {
}
