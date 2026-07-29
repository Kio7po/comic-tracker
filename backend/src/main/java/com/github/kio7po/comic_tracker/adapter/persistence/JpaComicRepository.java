package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;

public interface JpaComicRepository extends JpaRepository<Comic, Long>, ComicRepository {
}
