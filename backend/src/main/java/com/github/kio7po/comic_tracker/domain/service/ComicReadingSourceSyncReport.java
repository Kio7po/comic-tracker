package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;

/**
 * What a sync did, grouped by outcome.
 *
 * @param created sources that didn't exist and were created, already approved.
 * @param linked existing sources that got a reference to the provider, or whose reference now holds
 *        the id the provider reports.
 * @param updated linked sources whose origin changed.
 * @param skipped descriptors that were not applied.
 * @param failedProviders providers that couldn't list their sites.
 */
public record ComicReadingSourceSyncReport(List<SyncedSource> created, List<SyncedSource> linked,
        List<SyncedSource> updated, List<SkippedSource> skipped, List<DiscoveryOutcome.Failed> failedProviders) {

    public record SyncedSource(String providerKey, ComicReadingSource source) {
    }

    public record SkippedSource(String providerKey, ComicReadingSourceDescriptor descriptor, SkipReason reason) {
    }

    public enum SkipReason {
        /** The descriptor has no URL. */
        NO_URL,
        /** The descriptor's URL can't be reduced to an origin. */
        INVALID_URL,
        /** An earlier descriptor of the same provider has the same origin. */
        DUPLICATE_URL,
        /** The new origin of an already linked source belongs to another source. */
        URL_IN_USE
    }
}
