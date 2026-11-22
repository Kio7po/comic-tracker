package com.github.kio7po.comic_tracker.domain.port.source;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;

/**
 * @param externalId null when the site gives no identifier for the work.
 * @param url the work's current URL on the site, which may differ from the one passed to
 *            {@link ComicReadingProvider#fetch}.
 * @param author as the site writes it; null when it doesn't give one. Not stored in the entry: it only helps a user
 *            tell works apart.
 * @param status null when it isn't known; {@link ComicStatus#OTHER} only for a status the provider does report but
 *            that has no equivalent in {@link ComicStatus}. Not stored in the entry.
 * @param synopsis null when the site gives none. Not stored in the entry.
 */
public record ComicReadingEntryDetails(@Nullable String externalId, String url, String title,
        Integer availableChapters, Instant latestChapterAt, @Nullable String author, @Nullable ComicStatus status,
        @Nullable String synopsis) {
}
