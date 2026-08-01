package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.ComicMetadataSource;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicMetadataSourceRepository;

public interface JpaComicMetadataSourceRepository
        extends JpaRepository<ComicMetadataSource, Long>, ComicMetadataSourceRepository {
}
