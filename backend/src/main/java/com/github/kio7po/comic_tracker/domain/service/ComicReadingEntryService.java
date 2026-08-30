package com.github.kio7po.comic_tracker.domain.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.common.Slugifier;
import com.github.kio7po.comic_tracker.domain.common.UrlNormalizer;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntrySortField;
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
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDetails;

@Service
public class ComicReadingEntryService {

    private final ComicReadingEntryRepository comicReadingEntryRepository;
    private final ComicReadingSourceRepository comicReadingSourceRepository;
    private final ComicRepository comicRepository;
    private final ComicReadingSourceIconResolverRegistry iconResolverRegistry;
    private final ComicReadingProviderRegistry readingProviderRegistry;
    private final UserService userService;
    private final Duration ttl;
    private final Duration retryBackoff;

    public ComicReadingEntryService(ComicReadingEntryRepository comicReadingEntryRepository,
            ComicReadingSourceRepository comicReadingSourceRepository, ComicRepository comicRepository,
            ComicReadingSourceIconResolverRegistry iconResolverRegistry,
            ComicReadingProviderRegistry readingProviderRegistry, UserService userService,
            @Value("${comic.reading-entry.ttl}") Duration ttl,
            @Value("${comic.reading-entry.retry-backoff}") Duration retryBackoff) {
        this.comicReadingEntryRepository = comicReadingEntryRepository;
        this.comicReadingSourceRepository = comicReadingSourceRepository;
        this.comicRepository = comicRepository;
        this.iconResolverRegistry = iconResolverRegistry;
        this.readingProviderRegistry = readingProviderRegistry;
        this.userService = userService;
        this.ttl = ttl;
        this.retryBackoff = retryBackoff;
    }

    /**
     * @param url must already be a well-formed URL.
     * @param locale must already be a well-formed BCP 47 tag.
     */
    @Transactional
    public ComicReadingEntry submit(Long comicId, Long sourceId, String url, String locale, Long contributorId) {
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));
        ComicReadingSource source = comicReadingSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ComicReadingSourceNotFoundException(sourceId));
        User contributor = userService.findById(contributorId);

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
            String locale, Long contributorId) {
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));
        User contributor = userService.findById(contributorId);
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

        List<ComicReadingEntry> entries = status == null ? comicReadingEntryRepository.findByComic(comic)
                : comicReadingEntryRepository.findByComicAndStatus(comic, status);

        refreshStaleEntries(entries);

        return entries;
    }

    // The parallel phase only ever sees a plain url String, never the entity itself - a Hibernate
    // session (open-in-view keeps one bound to this request's thread) isn't thread-safe, so no JPA
    // access can happen on the virtual threads. Entities are only touched again once back on this
    // thread, after invokeAll returns, and persisted in one saveAll instead of one save() per entry.
    private void refreshStaleEntries(List<ComicReadingEntry> entries) {
        Instant now = Instant.now();
        List<ComicReadingEntry> stale = entries.stream().filter(entry -> needsRefresh(entry, now)).toList();
        if (stale.isEmpty()) {
            return;
        }

        List<Callable<FetchOutcome>> tasks = stale.stream()
                .<Callable<FetchOutcome>>map(entry -> () -> fetchOutcome(entry.getUrl())).toList();

        List<Future<FetchOutcome>> futures;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            futures = executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        // invokeAll returns futures in the same order as the submitted tasks, so index-pairing
        // them back with `stale` is safe.
        List<ComicReadingEntry> updated = new ArrayList<>(stale.size());
        for (int i = 0; i < stale.size(); i++) {
            updated.add(applyOutcome(stale.get(i), awaitOutcome(futures.get(i)), now));
        }
        comicReadingEntryRepository.saveAll(updated);
    }

    private boolean needsRefresh(ComicReadingEntry entry, Instant now) {
        return entry.getStatus() != ComicReadingEntryStatus.REJECTED
                && (entry.getLastFetchedAt() == null || now.isAfter(entry.getLastFetchedAt().plus(ttl)));
    }

    private FetchOutcome fetchOutcome(String url) {
        try {
            return new FetchOutcome(readingProviderRegistry.fetch(url), false);
        } catch (RuntimeException e) {
            // TODO: logs
            return new FetchOutcome(Optional.empty(), true);
        }
    }

    // fetchOutcome() never throws, so a failure here only means this thread was interrupted while waiting.
    // TODO: logs
    private FetchOutcome awaitOutcome(Future<FetchOutcome> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private ComicReadingEntry applyOutcome(ComicReadingEntry entry, FetchOutcome outcome, Instant now) {
        if (outcome.failed()) {
            entry.setLastFetchedAt(now.minus(ttl).plus(retryBackoff));
        } else {
            outcome.details().ifPresent(details -> applyFetchedDetails(entry, details));
            entry.setLastFetchedAt(now);
        }
        return entry;
    }

    private void applyFetchedDetails(ComicReadingEntry entry, ComicReadingSourceDetails details) {
        entry.setTitle(details.title());
        entry.setAvailableChapters(details.availableChapters());
        entry.setLatestChapterAt(details.latestChapterAt());
    }

    private record FetchOutcome(Optional<ComicReadingSourceDetails> details, boolean failed) {
    }

    public Page<ComicReadingEntry> findByStatusIn(List<ComicReadingEntryStatus> statuses,
            ComicReadingEntrySortField sortBy, SortDirection direction, int limit, int offset) {
        return comicReadingEntryRepository.findByStatusIn(statuses, sortBy, direction, limit, offset);
    }

    @Transactional
    public ComicReadingEntry approve(Long entryId, Long reviewerId) {
        ComicReadingEntry entry = findPendingEntry(entryId);

        ComicReadingSource source = entry.getSource();
        if (source.getStatus() != ComicReadingSourceStatus.APPROVED) {
            throw new ComicReadingSourceNotApprovedException(source.getId(), source.getStatus());
        }

        return resolve(entry, userService.findById(reviewerId), ComicReadingEntryStatus.APPROVED);
    }

    @Transactional
    public ComicReadingEntry reject(Long entryId, Long reviewerId) {
        return resolve(findPendingEntry(entryId), userService.findById(reviewerId), ComicReadingEntryStatus.REJECTED);
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
