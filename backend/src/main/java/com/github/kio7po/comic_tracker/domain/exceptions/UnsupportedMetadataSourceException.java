package com.github.kio7po.comic_tracker.domain.exceptions;

public class UnsupportedMetadataSourceException extends DomainException {

    private final String sourceSlug;

    public UnsupportedMetadataSourceException(String sourceSlug) {
        super("No active ComicMetadataProvider for source '" + sourceSlug + "'");
        this.sourceSlug = sourceSlug;
    }

    public String getSourceSlug() {
        return sourceSlug;
    }

}
