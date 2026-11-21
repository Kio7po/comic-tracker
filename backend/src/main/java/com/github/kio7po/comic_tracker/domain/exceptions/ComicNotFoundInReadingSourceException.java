package com.github.kio7po.comic_tracker.domain.exceptions;

public class ComicNotFoundInReadingSourceException extends DomainException {

    private final String url;

    public ComicNotFoundInReadingSourceException(String url) {
        super("The reading source has no comic at url '" + url + "'");
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

}
