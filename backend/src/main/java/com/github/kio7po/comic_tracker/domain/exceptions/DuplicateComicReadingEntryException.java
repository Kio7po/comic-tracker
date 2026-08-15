package com.github.kio7po.comic_tracker.domain.exceptions;

public class DuplicateComicReadingEntryException extends DomainException {

    private final Long comicId;
    private final Long sourceId;
    private final String url;

    public DuplicateComicReadingEntryException(Long comicId, Long sourceId, String url) {
        super("A ComicReadingEntry for comic " + comicId + ", source " + sourceId + " and url '" + url
                + "' already exists");
        this.comicId = comicId;
        this.sourceId = sourceId;
        this.url = url;
    }

    public Long getComicId() {
        return comicId;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getUrl() {
        return url;
    }

}
