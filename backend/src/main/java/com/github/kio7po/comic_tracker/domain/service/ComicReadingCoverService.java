package com.github.kio7po.comic_tracker.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingCoverUnavailableException;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverImage;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverProvider;

@Service
public class ComicReadingCoverService {

    private final List<ComicReadingCoverProvider> coverProviders;

    public ComicReadingCoverService(List<ComicReadingCoverProvider> coverProviders) {
        this.coverProviders = coverProviders;
    }

    /**
     * Asks the provider that handed out a cover id for that cover.
     *
     * @param externalCoverId the cover's identifier in that provider.
     * @return empty when no provider has that key, or the provider has no such cover.
     * @throws ComicReadingCoverUnavailableException if the provider couldn't answer.
     */
    public Optional<ComicReadingCoverImage> findCover(String providerKey, String externalCoverId) {
        Optional<ComicReadingCoverProvider> coverProvider = coverProviders.stream()
                .filter(candidate -> candidate.key().equals(providerKey))
                .findFirst();
        if (coverProvider.isEmpty()) {
            return Optional.empty();
        }
        try {
            return coverProvider.get().fetchCover(externalCoverId);
        } catch (RuntimeException e) {
            throw new ComicReadingCoverUnavailableException(providerKey, e);
        }
    }

}
