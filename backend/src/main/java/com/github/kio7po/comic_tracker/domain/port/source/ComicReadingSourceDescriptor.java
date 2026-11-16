package com.github.kio7po.comic_tracker.domain.port.source;

/**
 * A reading site a provider can serve.
 *
 * @param externalId the site's identifier in that provider.
 * @param url the site's origin.
 */
public record ComicReadingSourceDescriptor(String externalId, String name, String url) {
}
