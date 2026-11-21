package com.github.kio7po.comic_tracker.domain.port.source;

import java.util.List;
import java.util.Optional;

public interface ComicReadingProvider {

    /**
     * Stable identifier of this provider, persisted in the source and entry references.
     */
    String key();

    /**
     * @param url must already be a well-formed URL.
     * @throws RuntimeException if it can't tell, e.g. the service it relies on is unreachable.
     */
    boolean supports(String url);

    /**
     * The sites this provider can serve.
     */
    List<ComicReadingSourceDescriptor> discoverSources();

    /**
     * @return empty only when the site answered that the work doesn't exist.
     * @throws RuntimeException if it couldn't find out.
     */
    Optional<ComicReadingEntryDetails> fetch(ComicReadingSourceTarget source, ComicReadingEntryTarget entry);

    List<ComicReadingSearchResult> search(ComicReadingSourceTarget source, String keywords);
}
