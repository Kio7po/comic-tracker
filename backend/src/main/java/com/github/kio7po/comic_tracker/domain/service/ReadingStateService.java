package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ReadingState;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ReadingStateStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.ReadingStateNotFoundException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ReadingStateRepository;

@Service
public class ReadingStateService {

    private final ReadingStateRepository readingStateRepository;
    private final ComicRepository comicRepository;
    private final UserService userService;

    public ReadingStateService(ReadingStateRepository readingStateRepository, ComicRepository comicRepository,
            UserService userService) {
        this.readingStateRepository = readingStateRepository;
        this.comicRepository = comicRepository;
        this.userService = userService;
    }

    /**
     * @param chapters must be non-negative.
     */
    @Transactional
    public ReadingState create(Long userId, Long comicId, ReadingStateStatus status, int chapters) {
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
     * Replaces the full mutable state of an existing {@link ReadingState} in one call — the caller
     * sends the complete new state (status and chapters) rather than incremental deltas.
     *
     * @param chapters must be non-negative.
     */
    @Transactional
    public ReadingState update(Long userId, Long comicId, ReadingStateStatus status, int chapters) {
        ReadingState readingState = findExisting(userId, comicId);
        readingState.setStatus(status);
        readingState.setChapters(chapters);

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

}
