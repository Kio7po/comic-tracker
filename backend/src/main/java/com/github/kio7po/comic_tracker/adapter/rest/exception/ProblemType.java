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
    public static final String COMIC_NOT_FOUND = "urn:problem-type:comic-not-found";
    public static final String READING_SOURCE_NOT_FOUND = "urn:problem-type:reading-source-not-found";
    public static final String READING_ENTRY_NOT_FOUND = "urn:problem-type:reading-entry-not-found";
    public static final String DUPLICATE_READING_SOURCE = "urn:problem-type:duplicate-reading-source";
    public static final String DUPLICATE_READING_ENTRY = "urn:problem-type:duplicate-reading-entry";
    public static final String READING_ENTRY_ALREADY_REVIEWED = "urn:problem-type:reading-entry-already-reviewed";
    public static final String READING_SOURCE_NOT_APPROVED = "urn:problem-type:reading-source-not-approved";
    public static final String READING_SOURCE_ALREADY_REVIEWED = "urn:problem-type:reading-source-already-reviewed";
    public static final String READING_STATE_NOT_FOUND = "urn:problem-type:reading-state-not-found";
    public static final String READING_STATE_ALREADY_EXISTS = "urn:problem-type:reading-state-already-exists";

    private ProblemType() {
    }

}
