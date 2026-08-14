package com.github.kio7po.comic_tracker.domain.service;

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.common.Slugifier;
import com.github.kio7po.comic_tracker.domain.common.UrlNormalizer;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotApprovedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingEntryException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingSourceException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;

@Service
public class ComicReadingEntryService {

    private final ComicReadingEntryRepository comicReadingEntryRepository;
    private final ComicReadingSourceRepository comicReadingSourceRepository;
    private final ComicRepository comicRepository;
    private final ComicReadingSourceIconResolverRegistry iconResolverRegistry;

    public ComicReadingEntryService(ComicReadingEntryRepository comicReadingEntryRepository,
            ComicReadingSourceRepository comicReadingSourceRepository, ComicRepository comicRepository,
            ComicReadingSourceIconResolverRegistry iconResolverRegistry) {
        this.comicReadingEntryRepository = comicReadingEntryRepository;
        this.comicReadingSourceRepository = comicReadingSourceRepository;
        this.comicRepository = comicRepository;
        this.iconResolverRegistry = iconResolverRegistry;
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

        return createEntry(comic, source, UrlNormalizer.normalize(url), locale, contributor);
    }

    /**
     * For a site not registered as a {@link ComicReadingSource} yet.
     *
     * @param sourceUrl must already be a well-formed URL.
     * @param url must already be a well-formed URL.
     * @param locale must already be a well-formed BCP 47 tag.
     * @throws DuplicateComicReadingSourceException if {@code sourceUrl} is already registered.
     */
    @Transactional
    public ComicReadingEntry submitWithNewSource(Long comicId, String sourceName, String sourceUrl, String url,
            String locale, User contributor) {
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));
        ComicReadingSource source = createSource(sourceName, UrlNormalizer.normalizeOrigin(sourceUrl), contributor);

        return createEntry(comic, source, UrlNormalizer.normalize(url), locale, contributor);
    }

    private ComicReadingSource createSource(String name, String url, User contributor) {
        comicReadingSourceRepository.findByUrl(url).ifPresent(existing -> {
            throw new DuplicateComicReadingSourceException(url, existing.getId());
        });

        // TODO: slug is derived from `name`, not from `url` (the identity check above). Two
        // genuinely different sources whose names happen to slugify the same would collide on
        // the DB's UNIQUE(slug) constraint. Narrow edge case, not solved now.
        ComicReadingSource source = new ComicReadingSource();
        source.setSlug(Slugifier.slugify(name));
        source.setName(name);
        source.setUrl(url);
        source.setContributedBy(contributor);
        source.setIconUrl(iconResolverRegistry.resolveIconUrl(source).orElse(null));

        return comicReadingSourceRepository.save(source);
    }

    private ComicReadingEntry createEntry(Comic comic, ComicReadingSource source, String url, String locale,
            User contributor) {
        if (comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, url).isPresent()) {
            throw new DuplicateComicReadingEntryException(comic.getId(), source.getId(), url);
        }

        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setComic(comic);
        entry.setSource(source);
        entry.setUrl(url);
        entry.setLocale(locale);
        entry.setContributedBy(contributor);

        return comicReadingEntryRepository.save(entry);
    }

    public List<ComicReadingEntry> findByComic(Long comicId, @Nullable ComicReadingEntryStatus status) {
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));

        return status == null ? comicReadingEntryRepository.findByComic(comic)
                : comicReadingEntryRepository.findByComicAndStatus(comic, status);
    }

    @Transactional
    public ComicReadingEntry approve(Long entryId, User reviewer) {
        ComicReadingEntry entry = findPendingEntry(entryId);

        ComicReadingSource source = entry.getSource();
        if (source.getStatus() != ComicReadingSourceStatus.APPROVED) {
            throw new ComicReadingSourceNotApprovedException(source.getId(), source.getStatus());
        }

        return resolve(entry, reviewer, ComicReadingEntryStatus.APPROVED);
    }

    @Transactional
    public ComicReadingEntry reject(Long entryId, User reviewer) {
        return resolve(findPendingEntry(entryId), reviewer, ComicReadingEntryStatus.REJECTED);
    }

    private ComicReadingEntry findPendingEntry(Long entryId) {
        ComicReadingEntry entry = comicReadingEntryRepository.findById(entryId)
                .orElseThrow(() -> new ComicReadingEntryNotFoundException(entryId));

        if (entry.getStatus() != ComicReadingEntryStatus.PENDING) {
            throw new ComicReadingEntryAlreadyReviewedException(entryId, entry.getStatus());
        }

        return entry;
    }

    private ComicReadingEntry resolve(ComicReadingEntry entry, User reviewer, ComicReadingEntryStatus resolution) {
        entry.setStatus(resolution);
        entry.setReviewedBy(reviewer);
        entry.setReviewedAt(Instant.now());

        return comicReadingEntryRepository.save(entry);
    }

}
