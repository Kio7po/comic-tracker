package com.github.kio7po.comic_tracker.domain.exceptions;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid username/email or password");
    }

}
