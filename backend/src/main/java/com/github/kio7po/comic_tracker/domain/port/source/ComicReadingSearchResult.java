package com.github.kio7po.comic_tracker.domain.port.source;

import org.jspecify.annotations.Nullable;

/**
 * @param externalId null when the site gives no identifier for the work.
 * @param url null when the site doesn't give it in a search listing.
 */
public record ComicReadingSearchResult(@Nullable String externalId, String title, @Nullable String url) {
}
