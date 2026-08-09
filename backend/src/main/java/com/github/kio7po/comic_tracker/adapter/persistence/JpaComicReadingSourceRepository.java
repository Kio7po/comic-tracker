package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;

public interface JpaComicReadingSourceRepository
        extends JpaRepository<ComicReadingSource, Long>, ComicReadingSourceRepository {
}
