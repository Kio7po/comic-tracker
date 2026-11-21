package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;

public sealed interface DiscoveryOutcome {

    String providerKey();

    /**
     * A provider answered with the sites it serves.
     */
    record Discovered(String providerKey, List<ComicReadingSourceDescriptor> sources) implements DiscoveryOutcome {
    }

    /**
     * It couldn't be found out which sites the provider serves.
     */
    record Failed(String providerKey, Exception cause) implements DiscoveryOutcome {
    }
}
