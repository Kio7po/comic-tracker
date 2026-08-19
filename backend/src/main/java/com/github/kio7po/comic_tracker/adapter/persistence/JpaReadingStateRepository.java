package com.github.kio7po.comic_tracker.adapter.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.entities.ReadingState;
import com.github.kio7po.comic_tracker.domain.port.persistence.ReadingStateRepository;

public interface JpaReadingStateRepository extends JpaRepository<ReadingState, Long>, ReadingStateRepository {
}
