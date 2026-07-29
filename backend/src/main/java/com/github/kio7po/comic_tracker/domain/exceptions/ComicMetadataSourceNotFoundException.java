package com.github.kio7po.comic_tracker.domain.exceptions;

public class ComicMetadataSourceNotFoundException extends DomainException {

    private final String sourceSlug;

    public ComicMetadataSourceNotFoundException(String sourceSlug) {
        super("No ComicMetadataSource registered for slug '" + sourceSlug + "'");
        this.sourceSlug = sourceSlug;
    }

    public String getSourceSlug() {
        return sourceSlug;
    }

}
