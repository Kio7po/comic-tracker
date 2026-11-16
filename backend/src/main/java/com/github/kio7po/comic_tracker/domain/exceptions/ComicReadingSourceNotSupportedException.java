package com.github.kio7po.comic_tracker.domain.exceptions;

public class ComicReadingSourceNotSupportedException extends DomainException {

    private final Long sourceId;

    public ComicReadingSourceNotSupportedException(Long sourceId) {
        super("No ComicReadingProvider supports the ComicReadingSource with id " + sourceId);
        this.sourceId = sourceId;
    }

    public Long getSourceId() {
        return sourceId;
    }

}
