package com.github.kio7po.comic_tracker.domain.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceSortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotApprovedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotSupportedException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;

@Service
public class ComicReadingSourceService {

    private final ComicReadingSourceRepository comicReadingSourceRepository;
    private final ComicReadingEntryRepository comicReadingEntryRepository;
    private final UserService userService;
    private final ComicReadingProviderRegistry readingProviderRegistry;

    public ComicReadingSourceService(ComicReadingSourceRepository comicReadingSourceRepository,
            ComicReadingEntryRepository comicReadingEntryRepository, UserService userService,
            ComicReadingProviderRegistry readingProviderRegistry) {
        this.comicReadingSourceRepository = comicReadingSourceRepository;
        this.comicReadingEntryRepository = comicReadingEntryRepository;
        this.userService = userService;
        this.readingProviderRegistry = readingProviderRegistry;
    }

    public List<ComicReadingSource> findByStatusIn(List<ComicReadingSourceStatus> statuses,
            ComicReadingSourceSortField sortBy, SortDirection direction) {
        return comicReadingSourceRepository.findByStatusIn(statuses, sortBy, direction);
    }

    /**
     * Searches the source's site for works matching the keywords.
     *
     * @param keywords must not be blank.
     * @throws ComicReadingSourceNotFoundException if there is no source with that id.
     * @throws ComicReadingSourceNotApprovedException if the source isn't approved.
     * @throws ComicReadingSourceNotSupportedException if no provider serves the source.
     */
    public ComicReadingSearchListing search(Long sourceId, String keywords) {
        ComicReadingSource source = comicReadingSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ComicReadingSourceNotFoundException(sourceId));

        if (source.getStatus() != ComicReadingSourceStatus.APPROVED) {
            throw new ComicReadingSourceNotApprovedException(sourceId, source.getStatus());
        }

        return readingProviderRegistry.search(source, keywords);
    }

    @Transactional
    public ComicReadingSource approve(Long sourceId, Long reviewerId) {
        return resolve(findPendingSource(sourceId), userService.findById(reviewerId), ComicReadingSourceStatus.APPROVED);
    }

    /**
     * Rejects the source and any still-{@code PENDING} entries under this source: once the source itself is
     * rejected, those entries can never be approved ({@link ComicReadingEntryService#approve}), so leaving
     * them {@code PENDING} would strand them indefinitely instead of resolving them.
     */
    @Transactional
    public ComicReadingSource reject(Long sourceId, Long reviewerId) {
        User reviewer = userService.findById(reviewerId);
        ComicReadingSource source = resolve(findPendingSource(sourceId), reviewer, ComicReadingSourceStatus.REJECTED);
        rejectPendingEntries(source, reviewer);
        return source;
    }

    private void rejectPendingEntries(ComicReadingSource source, User reviewer) {
        Instant reviewedAt = Instant.now();
        List<ComicReadingEntry> pendingEntries = comicReadingEntryRepository.findBySourceAndStatus(source,
                ComicReadingEntryStatus.PENDING);

        for (ComicReadingEntry entry : pendingEntries) {
            entry.setStatus(ComicReadingEntryStatus.REJECTED);
            entry.setReviewedBy(reviewer);
            entry.setReviewedAt(reviewedAt);
            comicReadingEntryRepository.save(entry);
        }
    }

    private ComicReadingSource findPendingSource(Long sourceId) {
        ComicReadingSource source = comicReadingSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ComicReadingSourceNotFoundException(sourceId));

        if (source.getStatus() != ComicReadingSourceStatus.PENDING) {
            throw new ComicReadingSourceAlreadyReviewedException(sourceId, source.getStatus());
        }

        return source;
    }

    private ComicReadingSource resolve(ComicReadingSource source, User reviewer, ComicReadingSourceStatus resolution) {
        source.setStatus(resolution);
        source.setReviewedBy(reviewer);
        source.setReviewedAt(Instant.now());

        return comicReadingSourceRepository.save(source);
    }

}