package com.github.kio7po.comic_tracker.domain.exceptions;

public class ComicReadingSourceNotFoundException extends DomainException {

    private final Long sourceId;

    public ComicReadingSourceNotFoundException(Long sourceId) {
        super("No ComicReadingSource found with id " + sourceId);
        this.sourceId = sourceId;
    }

    public Long getSourceId() {
        return sourceId;
    }

}
