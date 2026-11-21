package com.github.kio7po.comic_tracker.domain.service;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;

/**
 * @param providerKey the provider that answered.
 */
public record FetchedEntry(String providerKey, ComicReadingEntryDetails details) {
}
