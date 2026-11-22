package com.github.kio7po.comic_tracker.domain.service;

import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundInReadingSourceException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingProviderFailedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotSupportedException;

public sealed interface FetchOutcome {

    /**
     * For a caller that has no use for anything but the work's data.
     *
     * @throws ComicNotFoundInReadingSourceException if the provider answered that the work doesn't exist.
     * @throws ComicReadingProviderFailedException if the provider couldn't answer.
     * @throws ComicReadingSourceNotSupportedException if the provider doesn't serve the source.
     */
    default FetchedEntry foundOrThrow(Long sourceId, String providerKey, String externalId) {
        return switch (this) {
            case Found(FetchedEntry found) -> found;
            case NotFound notFound -> throw new ComicNotFoundInReadingSourceException(sourceId, externalId);
            case Failed(Exception cause) -> throw new ComicReadingProviderFailedException(sourceId, providerKey, cause);
            case NotSupported notSupported -> throw new ComicReadingSourceNotSupportedException(sourceId);
        };
    }


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
