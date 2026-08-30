package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingProvider;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDetails;

@ExtendWith(MockitoExtension.class)
class ComicReadingProviderRegistryTest {

    private static final String URL = "https://example.com/berserk";

    @Mock
    private ComicReadingProvider unsupportingProvider;
    @Mock
    private ComicReadingProvider firstMatchingProvider;
    @Mock
    private ComicReadingProvider secondMatchingProvider;

    @Test
    void returnsEmptyWhenNoProviderSupportsTheUrl() {
        when(unsupportingProvider.supports(URL)).thenReturn(false);

        ComicReadingProviderRegistry registry = new ComicReadingProviderRegistry(List.of(unsupportingProvider));

        assertThat(registry.fetch(URL)).isEmpty();
    }

    @Test
    void usesTheFirstSupportingProviderInListOrder() {
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("Berserk", 374, null);
        when(unsupportingProvider.supports(URL)).thenReturn(false);
        when(firstMatchingProvider.supports(URL)).thenReturn(true);
        when(firstMatchingProvider.fetch(URL)).thenReturn(Optional.of(details));
        // secondMatchingProvider would also match, but shouldn't be consulted once one already did.

        ComicReadingProviderRegistry registry = new ComicReadingProviderRegistry(
                List.of(unsupportingProvider, firstMatchingProvider, secondMatchingProvider));

        assertThat(registry.fetch(URL)).contains(details);
    }

}
