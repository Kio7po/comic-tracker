package com.github.kio7po.comic_tracker.domain.exceptions;

public class EmailAlreadyExistsException extends DomainException {

    private final String email;

    public EmailAlreadyExistsException(String email) {
        super("Email '" + email + "' is already registered");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

}
