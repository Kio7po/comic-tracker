package com.github.kio7po.comic_tracker.domain.exceptions;

public class WeakPasswordException extends DomainException {

    private final int minLength;

    public WeakPasswordException(int minLength) {
        super("Password must be at least " + minLength + " characters long");
        this.minLength = minLength;
    }

    public int getMinLength() {
        return minLength;
    }

}
