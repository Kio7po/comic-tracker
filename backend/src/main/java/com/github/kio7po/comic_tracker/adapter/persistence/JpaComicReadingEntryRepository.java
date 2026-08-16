package com.github.kio7po.comic_tracker.adapter.persistence;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntrySortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;

public interface JpaComicReadingEntryRepository
        extends JpaRepository<ComicReadingEntry, Long>, ComicReadingEntryRepository {

    // comic/source are LAZY; this listing can span many rows across many comics, so fetch both
    // eagerly in the same query instead of risking an N+1 per row.
    @EntityGraph(attributePaths = { "comic", "source" })
    org.springframework.data.domain.Page<ComicReadingEntry> findByStatusIn(List<ComicReadingEntryStatus> statuses,
            Pageable pageable);

    // Keeps org.springframework.data.domain.{Sort,Pageable} (Spring Data types) out of the domain
    // port's signature: this default method is the adapter-side translation from the port's own
    // plain-Java sort/pagination criteria to what the derived finder above actually needs.
    @Override
    default Page<ComicReadingEntry> findByStatusIn(List<ComicReadingEntryStatus> statuses,
            ComicReadingEntrySortField sortBy, SortDirection direction, int limit, int offset) {
        String property = switch (sortBy) {
            case CREATED_AT -> "createdAt";
        };
        Sort.Direction sortDirection = switch (direction) {
            case ASC -> Sort.Direction.ASC;
            case DESC -> Sort.Direction.DESC;
        };
        Pageable pageable = PageRequest.of(offset / limit, limit, Sort.by(sortDirection, property));

        org.springframework.data.domain.Page<ComicReadingEntry> page = findByStatusIn(statuses, pageable);
        return new Page<>(page.getContent(), page.hasNext(), (int) page.getTotalElements());
    }

}