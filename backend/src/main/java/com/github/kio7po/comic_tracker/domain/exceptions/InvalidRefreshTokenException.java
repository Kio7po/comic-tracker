package com.github.kio7po.comic_tracker.domain.exceptions;

public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("Refresh token is invalid, expired or revoked");
    }

}
