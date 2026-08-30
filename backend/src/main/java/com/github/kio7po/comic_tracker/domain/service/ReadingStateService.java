package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ReadingState;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ReadingStateStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.InvalidPreferredReadingEntryException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateNotFoundException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ReadingStateRepository;

@Service
public class ReadingStateService {

    private final ReadingStateRepository readingStateRepository;
    private final ComicRepository comicRepository;
    private final ComicReadingEntryRepository comicReadingEntryRepository;
    private final UserService userService;

    public ReadingStateService(ReadingStateRepository readingStateRepository, ComicRepository comicRepository,
            ComicReadingEntryRepository comicReadingEntryRepository, UserService userService) {
        this.readingStateRepository = readingStateRepository;
        this.comicRepository = comicRepository;
        this.comicReadingEntryRepository = comicReadingEntryRepository;
        this.userService = userService;
    }

    /**
     * @param chapters must be non-negative.
     * @param preferredEntryId must belong to the same comic as {@code comicId}.
     */
    @Transactional
    public ReadingState create(Long userId, Long comicId, ReadingStateStatus status, int chapters,
            @Nullable String notes, @Nullable Long preferredEntryId) {
        User user = userService.findById(userId);
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));

        if (readingStateRepository.findByUserAndComic(user, comic).isPresent()) {
            throw new ReadingStateAlreadyExistsException(userId, comicId);
        }

        ReadingState readingState = new ReadingState();
        readingState.setUser(user);
        readingState.setComic(comic);
        readingState.setStatus(status);
        readingState.setChapters(chapters);
        readingState.setNotes(notes);
        readingState.setPreferredEntry(resolvePreferredEntry(comic, preferredEntryId));

        return readingStateRepository.save(readingState);
    }

    public Optional<ReadingState> findByUserAndComic(Long userId, Long comicId) {
        User user = userService.findById(userId);
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));

        return readingStateRepository.findByUserAndComic(user, comic);
    }

    public List<ReadingState> findByUser(Long userId) {
        return readingStateRepository.findByUser(userService.findById(userId));
    }

    /**
     * Replaces the full mutable state of an existing {@link ReadingState} in one call.
     * The caller sends the complete new state rather than incremental deltas.
     *
     * @param chapters must be non-negative.
     * @param preferredEntryId must belong to the same comic as the tracked {@code comicId}.
     */
    @Transactional
    public ReadingState update(Long userId, Long comicId, ReadingStateStatus status, int chapters,
            @Nullable String notes, @Nullable Long preferredEntryId) {
        ReadingState readingState = findExisting(userId, comicId);
        readingState.setStatus(status);
        readingState.setChapters(chapters);
        readingState.setNotes(notes);
        readingState.setPreferredEntry(resolvePreferredEntry(readingState.getComic(), preferredEntryId));

        return readingStateRepository.save(readingState);
    }

    @Transactional
    public void delete(Long userId, Long comicId) {
        readingStateRepository.delete(findExisting(userId, comicId));
    }

    private ReadingState findExisting(Long userId, Long comicId) {
        User user = userService.findById(userId);
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));

        return readingStateRepository.findByUserAndComic(user, comic)
                .orElseThrow(() -> new ReadingStateNotFoundException(userId, comicId));
    }

    private @Nullable ComicReadingEntry resolvePreferredEntry(Comic comic, @Nullable Long preferredEntryId) {
        if (preferredEntryId == null) {
            return null;
        }

        ComicReadingEntry entry = comicReadingEntryRepository.findById(preferredEntryId)
                .orElseThrow(() -> new ComicReadingEntryNotFoundException(preferredEntryId));
        if (!entry.getComic().getId().equals(comic.getId()) || entry.isRejected()) {
            throw new InvalidPreferredReadingEntryException(preferredEntryId, comic.getId());
        }

        return entry;
    }

}
