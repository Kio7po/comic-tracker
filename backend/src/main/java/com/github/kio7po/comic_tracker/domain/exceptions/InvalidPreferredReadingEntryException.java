package com.github.kio7po.comic_tracker.domain.exceptions;

public class InvalidPreferredReadingEntryException extends DomainException {

    private final Long entryId;
    private final Long comicId;

    public InvalidPreferredReadingEntryException(Long entryId, Long comicId) {
        super("ComicReadingEntry " + entryId + " does not belong to comic " + comicId);
        this.entryId = entryId;
        this.comicId = comicId;
    }

    public Long getEntryId() {
        return entryId;
    }

    public Long getComicId() {
        return comicId;
    }

}
