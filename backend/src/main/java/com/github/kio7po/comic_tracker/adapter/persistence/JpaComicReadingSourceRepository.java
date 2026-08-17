package com.github.kio7po.comic_tracker.adapter.persistence;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;

public interface JpaComicReadingSourceRepository
        extends JpaRepository<ComicReadingSource, Long>, ComicReadingSourceRepository {

    // contributedBy is LAZY; shared by the public picker (doesn't use it) and the moderation
    // listing (does), so fetch it eagerly here rather than risking an N+1 per row from moderation.
    @EntityGraph(attributePaths = { "contributedBy" })
    List<ComicReadingSource> findByStatusIn(List<ComicReadingSourceStatus> statuses, Sort sort);

    // Translation from the port's own plain-Java sort criteria to the Spring Data type
    // the derived finder above actually needs.
    @Override
    default List<ComicReadingSource> findByStatusIn(List<ComicReadingSourceStatus> statuses,
            ComicReadingSourceSortField sortBy, SortDirection direction) {
        String property = switch (sortBy) {
            case NAME -> "name";
            case CREATED_AT -> "createdAt";
        };
        Sort.Direction sortDirection = switch (direction) {
            case ASC -> Sort.Direction.ASC;
            case DESC -> Sort.Direction.DESC;
        };

        return findByStatusIn(statuses, Sort.by(sortDirection, property));
    }

}