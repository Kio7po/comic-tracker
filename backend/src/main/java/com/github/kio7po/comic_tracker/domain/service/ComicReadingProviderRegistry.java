package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
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
     * @param externalId must have been returned by an earlier call to the provider that supports {@code url},
     *                   or be null when none is known.
     */
    public Optional<ComicReadingSourceDetails> fetch(String url, @Nullable String externalId) {
        return providers.stream()
                .filter(provider -> provider.supports(url))
                .findFirst()
                .flatMap(provider -> provider.fetch(url, externalId)
                        .map(details -> withSupportedUrl(provider, details, url)));
    }

    /**
     * Whether {@link #fetch} can give an answer for this url, so that an empty result from it means the
     * provider found nothing rather than there being no provider at all.
     *
     * @param url must already be a well-formed URL.
     */
    public boolean supports(String url) {
        return providers.stream().anyMatch(provider -> provider.supports(url));
    }

    // A provider could hand back a URL on a host it doesn't serve; in that case keep the one we already had.
    private static ComicReadingSourceDetails withSupportedUrl(ComicReadingProvider provider,
            ComicReadingSourceDetails details, String fallbackUrl) {
        if (provider.supports(details.url())) {
            return details;
        }
        return new ComicReadingSourceDetails(details.externalId(), fallbackUrl, details.title(),
                details.availableChapters(), details.latestChapterAt());
    }

}
