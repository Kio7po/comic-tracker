package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * GraphQL answers 200 even when the operation failed; the failure is in {@code errors}.
 *
 * @param data the operation's result, null when it failed.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SuwayomiGraphQlResponseDto<T>(T data, List<Error> errors) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Error(String message) {
    }
}
