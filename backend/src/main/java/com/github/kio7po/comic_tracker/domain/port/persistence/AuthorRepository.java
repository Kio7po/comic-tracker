package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.Author;

public interface AuthorRepository {
    Optional<Author> findByName(String name);
    Author save(Author author);
}
