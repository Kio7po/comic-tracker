package com.github.kio7po.comic_tracker.domain.exceptions;

import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;

public class ComicReadingEntryAlreadyReviewedException extends DomainException {

    private final Long entryId;
    private final ComicReadingEntryStatus currentStatus;

    public ComicReadingEntryAlreadyReviewedException(Long entryId, ComicReadingEntryStatus currentStatus) {
        super("ComicReadingEntry " + entryId + " was already reviewed (status: " + currentStatus + ")");
        this.entryId = entryId;
        this.currentStatus = currentStatus;
    }

    public Long getEntryId() {
        return entryId;
    }

    public ComicReadingEntryStatus getCurrentStatus() {
        return currentStatus;
    }

}
