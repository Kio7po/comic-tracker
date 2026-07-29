package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.Genre;

public interface GenreRepository {
    Optional<Genre> findByName(String name);
    Genre save(Genre genre);
}
