package com.github.kio7po.comic_tracker.domain.port.source;

import java.util.List;
import java.util.Optional;

public interface ComicReadingProvider {
    boolean supports(String url);

    /**
     * @param url must already be a well-formed URL, and {@link #supports(String)} must return true for it.
     */
    Optional<ComicReadingSourceDetails> fetch(String url);

    List<ComicReadingSearchResult> search(String keywords);
}
