package com.github.kio7po.comic_tracker.adapter.rest.exception;

/**
 * Stable, per-exception RFC 9457 {@code type} identifiers. Lets clients identify the specific
 * problem instead of guessing it back from the HTTP status code, which is ambiguous whenever more
 * than one exception maps to the same status, or having to rely solely on the {@code detail} field.
 */
public final class ProblemType {

    public static final String USERNAME_ALREADY_EXISTS = "urn:problem-type:username-already-exists";
    public static final String EMAIL_ALREADY_EXISTS = "urn:problem-type:email-already-exists";
    public static final String WEAK_PASSWORD = "urn:problem-type:weak-password";
    public static final String INVALID_CREDENTIALS = "urn:problem-type:invalid-credentials";
    public static final String INVALID_REFRESH_TOKEN = "urn:problem-type:invalid-refresh-token";
    public static final String UNSUPPORTED_METADATA_SOURCE = "urn:problem-type:unsupported-metadata-source";
    public static final String METADATA_SOURCE_NOT_FOUND = "urn:problem-type:metadata-source-not-found";

    private ProblemType() {
    }

}
