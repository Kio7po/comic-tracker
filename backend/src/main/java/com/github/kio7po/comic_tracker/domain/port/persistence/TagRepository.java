package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.Tag;

public interface TagRepository {
    Optional<Tag> findByName(String name);
    Tag save(Tag tag);
}
