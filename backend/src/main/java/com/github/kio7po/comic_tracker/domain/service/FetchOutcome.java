package com.github.kio7po.comic_tracker.domain.service;

public sealed interface FetchOutcome {

    /**
     * A provider answered with the work's data.
     */
    record Found(FetchedEntry entry) implements FetchOutcome {
    }

    /**
     * A provider answered that the work doesn't exist.
     */
    record NotFound() implements FetchOutcome {
    }

    /**
     * It couldn't be found out whether the work exists.
     */
    record Failed(Exception cause) implements FetchOutcome {
    }

    /**
     * No provider serves the source.
     */
    record NotSupported() implements FetchOutcome {
    }
}
