package com.github.kio7po.comic_tracker.domain.exceptions;

/**
 * The work was asked of a provider either by its url or by its external id, and the provider answered that it
 * doesn't exist; the fields of the form not used are null.
 */
public class ComicNotFoundInReadingSourceException extends DomainException {

    private final String url;
    private final Long sourceId;
    private final String externalId;

    public ComicNotFoundInReadingSourceException(String url) {
        super("The reading source has no comic at url '" + url + "'");
        this.url = url;
        this.sourceId = null;
        this.externalId = null;
    }

    public ComicNotFoundInReadingSourceException(Long sourceId, String externalId) {
        super("The reading source " + sourceId + " has no comic with external id '" + externalId + "'");
        this.url = null;
        this.sourceId = sourceId;
        this.externalId = externalId;
    }

    public String getUrl() {
        return url;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getExternalId() {
        return externalId;
    }

}
