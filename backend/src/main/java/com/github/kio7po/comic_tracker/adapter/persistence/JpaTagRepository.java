package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.Tag;
import com.github.kio7po.comic_tracker.domain.port.persistence.TagRepository;

public interface JpaTagRepository extends JpaRepository<Tag, Long>, TagRepository {
}
