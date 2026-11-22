package com.github.kio7po.comic_tracker.domain.exceptions;

/**
 * A provider couldn't answer (network, rate limit, an error of its own), as opposed to answering that the work
 * doesn't exist.
 */
public class ComicReadingProviderFailedException extends DomainException {

    private final Long sourceId;
    private final String providerKey;

    public ComicReadingProviderFailedException(Long sourceId, String providerKey, Throwable cause) {
        super("The ComicReadingProvider '" + providerKey + "' failed to answer for the ComicReadingSource with id "
                + sourceId, cause);
        this.sourceId = sourceId;
        this.providerKey = providerKey;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getProviderKey() {
        return providerKey;
    }

}
