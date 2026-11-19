package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

/**
 * Suwayomi answered the HTTP request but reported the GraphQL operation as failed.
 */
class SuwayomiGraphQlException extends RuntimeException {

    SuwayomiGraphQlException(String message) {
        super(message);
    }
}
