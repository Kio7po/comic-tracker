package com.github.kio7po.comic_tracker.domain.port.source;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * @param externalId null when the site gives no identifier for the work.
 * @param url the work's current URL on the site, which may differ from the one passed to
 *            {@link ComicReadingProvider#fetch}.
 */
public record ComicReadingEntryDetails(@Nullable String externalId, String url, String title,
        Integer availableChapters, Instant latestChapterAt) {
}
