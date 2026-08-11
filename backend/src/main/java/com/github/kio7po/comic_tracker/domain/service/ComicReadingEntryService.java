package com.github.kio7po.comic_tracker.domain.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingEntryException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;

@Service
public class ComicReadingEntryService {

    private final ComicReadingEntryRepository comicReadingEntryRepository;
    private final ComicReadingSourceRepository comicReadingSourceRepository;
    private final ComicRepository comicRepository;

    public ComicReadingEntryService(ComicReadingEntryRepository comicReadingEntryRepository,
            ComicReadingSourceRepository comicReadingSourceRepository, ComicRepository comicRepository) {
        this.comicReadingEntryRepository = comicReadingEntryRepository;
        this.comicReadingSourceRepository = comicReadingSourceRepository;
        this.comicRepository = comicRepository;
    }

    /**
     * @param url must already be a well-formed URL.
     * @param locale must already be a well-formed BCP 47 tag.
     */
    @Transactional
    public ComicReadingEntry submit(Long comicId, Long sourceId, String url, String locale, User contributor) {
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));
        ComicReadingSource source = comicReadingSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ComicReadingSourceNotFoundException(sourceId));

        if (comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, url).isPresent()) {
            throw new DuplicateComicReadingEntryException(comicId, sourceId, url);
        }

        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setComic(comic);
        entry.setSource(source);
        entry.setUrl(url);
        entry.setLocale(locale);
        entry.setContributedBy(contributor);

        return comicReadingEntryRepository.save(entry);
    }

    @Transactional
    public ComicReadingEntry approve(Long entryId, User reviewer) {
        return review(entryId, reviewer, ComicReadingEntryStatus.APPROVED);
    }

    @Transactional
    public ComicReadingEntry reject(Long entryId, User reviewer) {
        return review(entryId, reviewer, ComicReadingEntryStatus.REJECTED);
    }

    private ComicReadingEntry review(Long entryId, User reviewer, ComicReadingEntryStatus resolution) {
        ComicReadingEntry entry = comicReadingEntryRepository.findById(entryId)
                .orElseThrow(() -> new ComicReadingEntryNotFoundException(entryId));

        if (entry.getStatus() != ComicReadingEntryStatus.PENDING) {
            throw new ComicReadingEntryAlreadyReviewedException(entryId, entry.getStatus());
        }

        entry.setStatus(resolution);
        entry.setReviewedBy(reviewer);
        entry.setReviewedAt(Instant.now());

        return comicReadingEntryRepository.save(entry);
    }

}
