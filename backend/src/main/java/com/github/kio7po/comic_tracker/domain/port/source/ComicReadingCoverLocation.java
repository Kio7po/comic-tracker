package com.github.kio7po.comic_tracker.domain.port.source;

/**
 * Where a browser can get the cover of a search result, as the provider that produced it sees it.
 */
public sealed interface ComicReadingCoverLocation {

    /**
     * The site publishes the cover at an address anyone can load.
     */
    record PublicUrl(String url) implements ComicReadingCoverLocation {
    }

    /**
     * The image can only be had through the provider, by asking it for this cover.
     *
     * @param externalCoverId the cover's identifier in the provider that serves it. It means nothing to anyone else.
     */
    record ServedByProvider(String externalCoverId) implements ComicReadingCoverLocation {
    }
}
