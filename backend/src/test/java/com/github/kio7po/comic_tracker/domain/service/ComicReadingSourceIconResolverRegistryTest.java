package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceIconResolver;

@ExtendWith(MockitoExtension.class)
class ComicReadingSourceIconResolverRegistryTest {

    @Mock
    private ComicReadingSourceIconResolver unsupportingResolver;
    @Mock
    private ComicReadingSourceIconResolver firstMatchingResolver;
    @Mock
    private ComicReadingSourceIconResolver secondMatchingResolver;

    @Test
    void returnsEmptyWhenNoResolverSupportsTheSource() {
        ComicReadingSource source = new ComicReadingSource();
        when(unsupportingResolver.supports(source)).thenReturn(false);

        ComicReadingSourceIconResolverRegistry registry = new ComicReadingSourceIconResolverRegistry(
                List.of(unsupportingResolver));

        assertThat(registry.resolveIconUrl(source)).isEmpty();
    }

    @Test
    void usesTheFirstSupportingResolverInListOrder() {
        ComicReadingSource source = new ComicReadingSource();
        when(unsupportingResolver.supports(source)).thenReturn(false);
        when(firstMatchingResolver.supports(source)).thenReturn(true);
        when(firstMatchingResolver.resolveIconUrl(source)).thenReturn(Optional.of("https://first.example/icon.png"));
        // secondMatchingResolver would also match, but shouldn't be consulted once one already did.

        ComicReadingSourceIconResolverRegistry registry = new ComicReadingSourceIconResolverRegistry(
                List.of(unsupportingResolver, firstMatchingResolver, secondMatchingResolver));

        assertThat(registry.resolveIconUrl(source)).contains("https://first.example/icon.png");
    }

}
