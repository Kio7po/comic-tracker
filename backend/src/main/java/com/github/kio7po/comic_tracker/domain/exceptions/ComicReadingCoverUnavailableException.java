package com.github.kio7po.comic_tracker.domain.exceptions;

/**
 * A provider couldn't serve a cover (network, an error of its own), as opposed to answering that it has none.
 */
public class ComicReadingCoverUnavailableException extends DomainException {

    private final String providerKey;

    public ComicReadingCoverUnavailableException(String providerKey, Throwable cause) {
        super("The ComicReadingProvider '" + providerKey + "' couldn't serve the cover", cause);
        this.providerKey = providerKey;
    }

    public String getProviderKey() {
        return providerKey;
    }

}
