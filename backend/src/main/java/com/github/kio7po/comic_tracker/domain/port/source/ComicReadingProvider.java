package com.github.kio7po.comic_tracker.domain.port.source;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

public interface ComicReadingProvider {
    boolean supports(String url);

    /**
     * @param url must already be a well-formed URL, and {@link #supports(String)} must return true for it.
     * @param externalId the work's identifier as returned by a previous {@link #fetch} or {@link #search}
     *                   of this same provider, or null when none is known.
     */
    Optional<ComicReadingSourceDetails> fetch(String url, @Nullable String externalId);

    List<ComicReadingSearchResult> search(String keywords);
}
