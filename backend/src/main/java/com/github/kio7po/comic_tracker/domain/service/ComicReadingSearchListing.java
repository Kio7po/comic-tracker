package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;

/**
 * What a search over one source found.
 *
 * @param providerKey the provider that ran the search; the external ids of the results are only valid for it.
 */
public record ComicReadingSearchListing(String providerKey, List<ComicReadingSearchResult> results) {
}
