package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;

@Service
public class ComicReadingSourceService {

    private final ComicReadingSourceRepository comicReadingSourceRepository;

    public ComicReadingSourceService(ComicReadingSourceRepository comicReadingSourceRepository) {
        this.comicReadingSourceRepository = comicReadingSourceRepository;
    }

    /**
     * Excludes {@link ComicReadingSourceStatus#REJECTED} sources only; pending ones are still
     * selectable so contributors can find and reuse a source someone else already proposed.
     */
    public List<ComicReadingSource> findSelectable() {
        return comicReadingSourceRepository.findByStatusNotOrderByNameAsc(ComicReadingSourceStatus.REJECTED);
    }

}