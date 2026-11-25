package com.github.kio7po.comic_tracker.domain.port.source;

import java.util.Optional;

/**
 * An optional capability of a provider: serving the covers it can't give as a public address. It is a port of its
 * own so that {@link ComicReadingProvider} doesn't grow a method most providers would leave empty.
 */
public interface ComicReadingCoverProvider {

    /**
     * @return the key of the {@link ComicReadingProvider} that hands out the cover ids this one serves.
     */
    String key();

    /**
     * @param externalCoverId the provider's own identifier of the cover, one it put in a
     *            {@link ComicReadingCoverLocation.ServedByProvider}.
     * @return empty when the provider answered that it has no such cover.
     * @throws RuntimeException if it couldn't find out (network, an error of its own, an id it can't read).
     */
    Optional<ComicReadingCoverImage> fetchCover(String externalCoverId);

}
