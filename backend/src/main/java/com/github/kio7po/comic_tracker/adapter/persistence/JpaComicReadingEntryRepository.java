package com.github.kio7po.comic_tracker.adapter.persistence;

import java.util.List;

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

    // comic/source/contributedBy are LAZY; this listing can span many rows across many comics, so
    // fetch all of them eagerly in the same query instead of risking an N+1 per row.
    @EntityGraph(attributePaths = { "comic", "source", "contributedBy" })
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
        // Spring Data's Pageable is page-index based, but the port's contract is a raw offset that
        // isn't guaranteed to be a multiple of limit, PageRequest.of(offset / limit, ...) would
        // silently truncate to the wrong window whenever it isn't. Use an unpaged/offset query via
        // a manual Pageable instead so an arbitrary offset is honored exactly.
        Pageable pageable = new OffsetPageable(offset, limit, Sort.by(sortDirection, property));

        org.springframework.data.domain.Page<ComicReadingEntry> page = findByStatusIn(statuses, pageable);
        // Not page.hasNext(): that's derived from the Pageable's page number, which OffsetPageable
        // only approximates for a non-page-aligned offset. This is exact for any offset.
        boolean existMoreItems = offset + page.getNumberOfElements() < page.getTotalElements();
        return new Page<>(page.getContent(), existMoreItems, (int) page.getTotalElements());
    }

    // List<ComicReadingEntry> avoids an erasure clash with JpaRepository's own generic
    // saveAll(Iterable<S>), so Spring Data doesn't auto-implement this overload - forward to the
    // real one explicitly. The cast is what selects it over this same method (a List is an
    // Iterable, so without it Java would just call itself here).
    @Override
    default List<ComicReadingEntry> saveAll(List<ComicReadingEntry> entries) {
        return saveAll((Iterable<ComicReadingEntry>) entries);
    }

}