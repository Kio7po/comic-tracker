package com.github.kio7po.comic_tracker.domain.port.source;

import org.jspecify.annotations.Nullable;

/**
 * A source as seen by one provider.
 *
 * @param url the source's origin.
 * @param externalId the source's reference in the provider receiving it, null when it isn't linked to it yet.
 */
public record ComicReadingSourceTarget(String url, @Nullable String externalId) {
}
