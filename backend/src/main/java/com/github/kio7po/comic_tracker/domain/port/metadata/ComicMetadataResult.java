package com.github.kio7po.comic_tracker.domain.port.metadata;

import com.github.kio7po.comic_tracker.domain.entities.Comic;

public class ComicMetadataResult {
    private String sourceSlug;
    private String externalId;
    private Comic comic;

    public ComicMetadataResult(String sourceSlug, String externalId, Comic comic) {
        this.sourceSlug = sourceSlug;
        this.externalId = externalId;
        this.comic = comic;
    }

    public String getSourceSlug() {
        return sourceSlug;
    }

    public void setSourceSlug(String sourceSlug) {
        this.sourceSlug = sourceSlug;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Comic getComic() {
        return comic;
    }

    public void setComic(Comic comic) {
        this.comic = comic;
    }
    
}
