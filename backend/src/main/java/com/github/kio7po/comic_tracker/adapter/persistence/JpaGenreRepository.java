package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.Genre;
import com.github.kio7po.comic_tracker.domain.port.persistence.GenreRepository;

public interface JpaGenreRepository extends JpaRepository<Genre, Long>, GenreRepository {
}
