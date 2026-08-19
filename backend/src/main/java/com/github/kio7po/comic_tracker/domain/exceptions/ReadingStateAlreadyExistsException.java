package com.github.kio7po.comic_tracker.domain.exceptions;

public class ReadingStateAlreadyExistsException extends DomainException {

    private final Long userId;
    private final Long comicId;

    public ReadingStateAlreadyExistsException(Long userId, Long comicId) {
        super("A ReadingState for user " + userId + " and comic " + comicId + " already exists");
        this.userId = userId;
        this.comicId = comicId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getComicId() {
        return comicId;
    }

}
