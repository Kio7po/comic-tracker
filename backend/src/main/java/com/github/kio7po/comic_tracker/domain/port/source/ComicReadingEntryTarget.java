package com.github.kio7po.comic_tracker.domain.port.source;

import org.jspecify.annotations.Nullable;

/**
 * The work asked of a provider. At least one of the two must be set.
 *
 * @param url the work's address on the site.
 * @param externalId the work's identifier in the provider receiving it.
 */
public record ComicReadingEntryTarget(@Nullable String url, @Nullable String externalId) {

    public ComicReadingEntryTarget {
        if (url == null && externalId == null) {
            throw new IllegalArgumentException("url and externalId can't both be null");
        }
    }
}
