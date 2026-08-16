package com.github.kio7po.comic_tracker.domain.exceptions;

import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;

public class ComicReadingSourceAlreadyReviewedException extends DomainException {

    private final Long sourceId;
    private final ComicReadingSourceStatus currentStatus;

    public ComicReadingSourceAlreadyReviewedException(Long sourceId, ComicReadingSourceStatus currentStatus) {
        super("ComicReadingSource " + sourceId + " was already reviewed (status: " + currentStatus + ")");
        this.sourceId = sourceId;
        this.currentStatus = currentStatus;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public ComicReadingSourceStatus getCurrentStatus() {
        return currentStatus;
    }

}