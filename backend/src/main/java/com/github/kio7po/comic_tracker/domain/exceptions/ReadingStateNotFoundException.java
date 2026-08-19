package com.github.kio7po.comic_tracker.domain.exceptions;

public class ReadingStateNotFoundException extends DomainException {

    private final Long userId;
    private final Long comicId;

    public ReadingStateNotFoundException(Long userId, Long comicId) {
        super("No ReadingState found for user " + userId + " and comic " + comicId);
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
