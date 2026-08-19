package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.List;
import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ReadingState;
import com.github.kio7po.comic_tracker.domain.entities.User;

public interface ReadingStateRepository {
    Optional<ReadingState> findByUserAndComic(User user, Comic comic);
    List<ReadingState> findByUser(User user);
    ReadingState save(ReadingState readingState);
    void delete(ReadingState readingState);
}
