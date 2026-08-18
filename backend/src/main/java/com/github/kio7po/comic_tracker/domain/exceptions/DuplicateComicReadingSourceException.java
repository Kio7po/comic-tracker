package com.github.kio7po.comic_tracker.domain.exceptions;

public class DuplicateComicReadingSourceException extends DomainException {

    private final String url;
    private final Long existingSourceId;

    public DuplicateComicReadingSourceException(String url, Long existingSourceId) {
        super("A ComicReadingSource with url '" + url + "' already exists (id " + existingSourceId + ")");
        this.url = url;
        this.existingSourceId = existingSourceId;
    }

    public String getUrl() {
        return url;
    }

    public Long getExistingSourceId() {
        return existingSourceId;
    }

}
