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

        assertThat(registry.fetch(URL, null)).isEmpty();
    }

    @Test
    void usesTheFirstSupportingProviderInListOrder() {
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("42", URL, "Berserk", 374, null);
        when(unsupportingProvider.supports(URL)).thenReturn(false);
        when(firstMatchingProvider.supports(URL)).thenReturn(true);
        when(firstMatchingProvider.fetch(URL, null)).thenReturn(Optional.of(details));
        // secondMatchingProvider would also match, but shouldn't be consulted once one already did.

        ComicReadingProviderRegistry registry = new ComicReadingProviderRegistry(
                List.of(unsupportingProvider, firstMatchingProvider, secondMatchingProvider));

        assertThat(registry.fetch(URL, null)).contains(details);
    }

    @Test
    void passesTheExternalIdThroughToTheSupportingProvider() {
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("42", URL, "Berserk", 374, null);
        when(firstMatchingProvider.supports(URL)).thenReturn(true);
        when(firstMatchingProvider.fetch(URL, "42")).thenReturn(Optional.of(details));

        ComicReadingProviderRegistry registry = new ComicReadingProviderRegistry(List.of(firstMatchingProvider));

        assertThat(registry.fetch(URL, "42")).contains(details);
    }

    @Test
    void keepsTheNewUrlWhenTheProviderReturnsOneItSupports() {
        // Propósito: si el provider devuelve una URL distinta a la pedida y sigue siendo de un sitio que
        // él soporta, el registro la respeta (la obra cambió de ruta).
        //
        // Flujo: se pide URL; el provider responde con "berserk-renamed"; supports() dice que sí la
        // soporta; el resultado conserva la URL nueva.
        String movedUrl = "https://example.com/berserk-renamed";
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("42", movedUrl, "Berserk", 374, null);
        when(firstMatchingProvider.supports(URL)).thenReturn(true);
        when(firstMatchingProvider.supports(movedUrl)).thenReturn(true);
        when(firstMatchingProvider.fetch(URL, "42")).thenReturn(Optional.of(details));

        ComicReadingProviderRegistry registry = new ComicReadingProviderRegistry(List.of(firstMatchingProvider));

        assertThat(registry.fetch(URL, "42")).contains(details);
    }

    @Test
    void keepsTheRequestedUrlWhenTheProviderReturnsOneItDoesNotSupport() {
        // Propósito: un provider no puede mandar a los usuarios a otro dominio: si devuelve una URL de
        // un sitio que él no soporta, el registro la descarta y deja la que se pidió.
        //
        // Flujo: se pide URL; el provider responde con una URL de otro dominio ("elsewhere.example");
        // supports() dice que no la soporta; el resultado lleva la URL pedida y el resto de datos
        // intactos.
        String foreignUrl = "https://elsewhere.example/berserk";
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("42", foreignUrl, "Berserk", 374, null);
        when(firstMatchingProvider.supports(URL)).thenReturn(true);
        when(firstMatchingProvider.supports(foreignUrl)).thenReturn(false);
        when(firstMatchingProvider.fetch(URL, "42")).thenReturn(Optional.of(details));

        ComicReadingProviderRegistry registry = new ComicReadingProviderRegistry(List.of(firstMatchingProvider));

        assertThat(registry.fetch(URL, "42")).contains(new ComicReadingSourceDetails("42", URL, "Berserk", 374, null));
    }

    @Test
    void supports_isTrueWhenAnyProviderSupportsTheUrl() {
        when(unsupportingProvider.supports(URL)).thenReturn(false);
        when(firstMatchingProvider.supports(URL)).thenReturn(true);

        ComicReadingProviderRegistry registry = new ComicReadingProviderRegistry(
                List.of(unsupportingProvider, firstMatchingProvider));

        assertThat(registry.supports(URL)).isTrue();
    }

    @Test
    void supports_isFalseWhenNoProviderSupportsTheUrl() {
        when(unsupportingProvider.supports(URL)).thenReturn(false);

        ComicReadingProviderRegistry registry = new ComicReadingProviderRegistry(List.of(unsupportingProvider));

        assertThat(registry.supports(URL)).isFalse();
    }

}
