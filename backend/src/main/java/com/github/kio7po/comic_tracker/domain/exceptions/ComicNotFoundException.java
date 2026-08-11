package com.github.kio7po.comic_tracker.domain.exceptions;

public class ComicNotFoundException extends DomainException {

    private final Long comicId;

    public ComicNotFoundException(Long comicId) {
        super("No Comic found with id " + comicId);
        this.comicId = comicId;
    }

    public Long getComicId() {
        return comicId;
    }

}
