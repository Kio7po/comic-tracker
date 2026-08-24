package com.github.kio7po.comic_tracker.adapter.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.ReadingState;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.port.persistence.ReadingStateRepository;

public interface JpaReadingStateRepository extends JpaRepository<ReadingState, Long>, ReadingStateRepository {

    // comic is LAZY, and alternativeTitles is itself a separate LAZY collection on Comic; this
    // listing spans many different comics for the response DTO, so fetch both eagerly here
    // instead of risking an N+1 per row.
    @Override
    @EntityGraph(attributePaths = { "comic", "comic.alternativeTitles" })
    List<ReadingState> findByUser(User user);

}
