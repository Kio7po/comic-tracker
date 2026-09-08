package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingProvider;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDetails;

@Component
public class ComicReadingProviderRegistry {

    private final List<ComicReadingProvider> providers;

    public ComicReadingProviderRegistry(List<ComicReadingProvider> providers) {
        this.providers = providers;
    }

    /**
     * @param url must already be a well-formed URL.
     */
    public Optional<ComicReadingSourceDetails> fetch(String url) {
        return providers.stream()
                .filter(provider -> provider.supports(url))
                .findFirst()
                .flatMap(provider -> provider.fetch(url));
    }

}
