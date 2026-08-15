package com.github.kio7po.comic_tracker.domain.exceptions;

public class ComicReadingEntryNotFoundException extends DomainException {

    private final Long entryId;

    public ComicReadingEntryNotFoundException(Long entryId) {
        super("No ComicReadingEntry found with id " + entryId);
        this.entryId = entryId;
    }

    public Long getEntryId() {
        return entryId;
    }

}
