package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.Comic;

public interface ComicRepository {
    Optional<Comic> findById(Long id);
    Optional<Comic> findBySlug(String slug);
    Comic save(Comic comic);
}
