package com.github.kio7po.comic_tracker.domain.exceptions;

public class UsernameAlreadyExistsException extends DomainException {

    private final String username;

    public UsernameAlreadyExistsException(String username) {
        super("Username '" + username + "' is already taken");
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

}
