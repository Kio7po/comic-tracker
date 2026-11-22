package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntryProviderReference;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.enums.ComicStatus;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSourceProviderReference;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotSupportedException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryProviderReferenceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceProviderReferenceRepository;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryTarget;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingProvider;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceTarget;

@ExtendWith(MockitoExtension.class)
class ComicReadingProviderRegistryTest {

    private static final String SOURCE_URL = "https://example.com";
    private static final String URL = "https://example.com/berserk";

    @Mock
    private ComicReadingProvider first;
    @Mock
    private ComicReadingProvider second;
    @Mock
    private ComicReadingSourceProviderReferenceRepository sourceReferenceRepository;
    @Mock
    private ComicReadingEntryProviderReferenceRepository entryReferenceRepository;

    private ComicReadingProviderRegistry registry;

    @BeforeEach
    void setUp() {
        lenient().when(first.key()).thenReturn("first");
        lenient().when(second.key()).thenReturn("second");
        registry = new ComicReadingProviderRegistry(List.of(first, second), sourceReferenceRepository,
                entryReferenceRepository);
    }

    private static ComicReadingSource source(Long id, String url) {
        ComicReadingSource source = new ComicReadingSource();
        source.setId(id);
        source.setUrl(url);
        return source;
    }

    private static ComicReadingEntry entry(Long id, ComicReadingSource source, String url) {
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setId(id);
        entry.setSource(source);
        entry.setUrl(url);
        return entry;
    }

    private static ComicReadingSourceProviderReference sourceReference(ComicReadingSource source,
            String providerKey, String externalId) {
        ComicReadingSourceProviderReference reference = new ComicReadingSourceProviderReference();
        reference.setSource(source);
        reference.setProviderKey(providerKey);
        reference.setExternalId(externalId);
        return reference;
    }

    private static ComicReadingEntryProviderReference entryReference(ComicReadingEntry entry, String providerKey,
            String externalId) {
        ComicReadingEntryProviderReference reference = new ComicReadingEntryProviderReference();
        reference.setEntry(entry);
        reference.setProviderKey(providerKey);
        reference.setExternalId(externalId);
        return reference;
    }

    private static ComicReadingEntryDetails details(String url) {
        return new ComicReadingEntryDetails("42", url, "Berserk", 374, null, null, null, null);
    }

    private static FetchOutcome found(String providerKey, ComicReadingEntryDetails details) {
        return new FetchOutcome.Found(new FetchedEntry(providerKey, details));
    }

    @Test
    void fetch_usesTheProviderTheSourceIsLinkedToAndPassesItsReference() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(URL, null);
        when(sourceReferenceRepository.findBySourceIn(List.of(source)))
                .thenReturn(List.of(sourceReference(source, "second", "s-9")));
        when(second.fetch(new ComicReadingSourceTarget(SOURCE_URL, "s-9"), target))
                .thenReturn(Optional.of(details(URL)));
        when(second.supports(URL)).thenReturn(true);

        FetchOutcome outcome = registry.fetch(source, target);

        assertThat(outcome).isEqualTo(found("second", details(URL)));
        // La referencia de la fuente ya decide el provider: el host no se consulta.
        verify(first, never()).supports(any());
    }

    @Test
    void fetch_followsTheProviderOrderWhenTheSourceIsLinkedToSeveral() {
        // Propósito: si una fuente tiene referencia en más de un provider, gana el primero en el orden en
        // que están registrados los providers.
        //
        // Flujo: la fuente está vinculada a "first" y a "second". Se pide la obra y solo se llama a
        // "first"; a "second" no se le pide nada.
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(URL, null);
        when(sourceReferenceRepository.findBySourceIn(List.of(source))).thenReturn(
                List.of(sourceReference(source, "second", "s-9"), sourceReference(source, "first", "f-1")));
        when(first.fetch(new ComicReadingSourceTarget(SOURCE_URL, "f-1"), target))
                .thenReturn(Optional.of(details(URL)));
        when(first.supports(URL)).thenReturn(true);

        FetchOutcome outcome = registry.fetch(source, target);

        assertThat(outcome).isEqualTo(found("first", details(URL)));
        verify(second, never()).fetch(any(), any());
    }

    @Test
    void fetch_fallsBackToTheHostWhenTheSourceIsNotLinked() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(URL, null);
        when(first.supports(SOURCE_URL)).thenReturn(false);
        when(second.supports(SOURCE_URL)).thenReturn(true);
        when(second.supports(URL)).thenReturn(true);
        when(second.fetch(new ComicReadingSourceTarget(SOURCE_URL, null), target))
                .thenReturn(Optional.of(details(URL)));

        FetchOutcome outcome = registry.fetch(source, target);

        assertThat(outcome).isEqualTo(found("second", details(URL)));
    }

    @Test
    void fetch_returnsNotSupportedWhenNoProviderServesTheSource() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        when(first.supports(SOURCE_URL)).thenReturn(false);
        when(second.supports(SOURCE_URL)).thenReturn(false);

        FetchOutcome outcome = registry.fetch(source, new ComicReadingEntryTarget(URL, null));

        assertThat(outcome).isEqualTo(new FetchOutcome.NotSupported());
    }

    @Test
    void fetch_returnsFailedWhenCheckingTheHostFails() {
        // Propósito: si un provider no sabe decir si sirve el sitio (p. ej. su servicio está caído), no se
        // afirma que ningún provider lo sirve: es un fallo, no "no soportado".
        //
        // Flujo: la fuente no está vinculada, así que se pregunta por el host; supports lanza una excepción
        // y el resultado es Failed con esa misma excepción.
        ComicReadingSource source = source(1L, SOURCE_URL);
        RuntimeException error = new RuntimeException("service is down");
        when(first.supports(SOURCE_URL)).thenThrow(error);

        FetchOutcome outcome = registry.fetch(source, new ComicReadingEntryTarget(URL, null));

        assertThat(outcome).isEqualTo(new FetchOutcome.Failed(error));
    }

    @Test
    void fetch_returnsNotFoundWhenTheProviderFindsNothing() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(URL, null);
        when(first.supports(SOURCE_URL)).thenReturn(true);
        when(first.fetch(new ComicReadingSourceTarget(SOURCE_URL, null), target)).thenReturn(Optional.empty());

        FetchOutcome outcome = registry.fetch(source, target);

        assertThat(outcome).isEqualTo(new FetchOutcome.NotFound());
    }

    @Test
    void fetch_returnsFailedWhenTheProviderThrows() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(URL, null);
        RuntimeException error = new RuntimeException("site is down");
        when(first.supports(SOURCE_URL)).thenReturn(true);
        when(first.fetch(new ComicReadingSourceTarget(SOURCE_URL, null), target)).thenThrow(error);

        FetchOutcome outcome = registry.fetch(source, target);

        assertThat(outcome).isEqualTo(new FetchOutcome.Failed(error));
    }

    @Test
    void fetch_keepsTheNewUrlWhenTheProviderReturnsOneItSupports() {
        // Propósito: si el provider devuelve una URL distinta a la pedida y sigue siendo de un sitio que
        // él soporta, el registro la respeta (la obra cambió de ruta).
        //
        // Flujo: se pide URL; el provider responde con "berserk-renamed"; supports() dice que sí la
        // soporta; el resultado conserva la URL nueva.
        String movedUrl = "https://example.com/berserk-renamed";
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(URL, "42");
        when(sourceReferenceRepository.findBySourceIn(List.of(source)))
                .thenReturn(List.of(sourceReference(source, "first", "f-1")));
        when(first.fetch(new ComicReadingSourceTarget(SOURCE_URL, "f-1"), target))
                .thenReturn(Optional.of(details(movedUrl)));
        when(first.supports(movedUrl)).thenReturn(true);

        FetchOutcome outcome = registry.fetch(source, target);

        assertThat(outcome).isEqualTo(found("first", details(movedUrl)));
    }

    @Test
    void fetch_keepsTheRequestedUrlWhenTheProviderReturnsOneItDoesNotSupport() {
        // Propósito: un provider no puede mandar a los usuarios a otro dominio: si devuelve una URL de
        // un sitio que él no soporta, el registro la descarta y deja la que se pidió.
        //
        // Flujo: se pide URL; el provider responde con una URL de otro dominio ("elsewhere.example");
        // supports() dice que no la soporta; el resultado lleva la URL pedida y el resto de datos
        // intactos.
        String foreignUrl = "https://elsewhere.example/berserk";
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(URL, "42");
        when(sourceReferenceRepository.findBySourceIn(List.of(source)))
                .thenReturn(List.of(sourceReference(source, "first", "f-1")));
        when(first.fetch(new ComicReadingSourceTarget(SOURCE_URL, "f-1"), target))
                .thenReturn(Optional.of(details(foreignUrl)));
        when(first.supports(foreignUrl)).thenReturn(false);

        FetchOutcome outcome = registry.fetch(source, target);

        assertThat(outcome).isEqualTo(found("first", details(URL)));
    }

    @Test
    void fetch_keepsTheRequestedUrlWhenCheckingTheReturnedUrlFails() {
        // Propósito: si no se puede comprobar si el provider soporta la URL que devolvió, se conserva la
        // pedida en lugar de fiarse de ella; y esa comprobación fallida no convierte en fallo una
        // respuesta que sí llegó.
        //
        // Flujo: el provider devuelve una URL de otro dominio y supports lanza al preguntar por ella. El
        // resultado es Found con la URL pedida.
        String foreignUrl = "https://elsewhere.example/berserk";
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(URL, "42");
        when(sourceReferenceRepository.findBySourceIn(List.of(source)))
                .thenReturn(List.of(sourceReference(source, "first", "f-1")));
        when(first.fetch(new ComicReadingSourceTarget(SOURCE_URL, "f-1"), target))
                .thenReturn(Optional.of(details(foreignUrl)));
        when(first.supports(foreignUrl)).thenThrow(new RuntimeException("service is down"));

        FetchOutcome outcome = registry.fetch(source, target);

        assertThat(outcome).isEqualTo(found("first", details(URL)));
    }

    @Test
    void fetch_keepsTheReturnedUrlWhenTheTargetHasNoUrl() {
        // Propósito: cuando se pide una obra solo por su id, no hay URL pedida que conservar, así que la
        // que devuelve el provider se acepta tal cual y no se comprueba.
        //
        // Flujo: el target lleva solo el externalId; el provider devuelve una URL y el resultado la
        // mantiene sin llamar a supports.
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(null, "42");
        when(sourceReferenceRepository.findBySourceIn(List.of(source)))
                .thenReturn(List.of(sourceReference(source, "first", "f-1")));
        when(first.fetch(new ComicReadingSourceTarget(SOURCE_URL, "f-1"), target))
                .thenReturn(Optional.of(details(URL)));

        FetchOutcome outcome = registry.fetch(source, target);

        assertThat(outcome).isEqualTo(found("first", details(URL)));
        verify(first, never()).supports(any());
    }

    @Test
    void fetchEntry_asksTheProviderForTheEntryUrlAndItsStoredId() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntry entry = entry(10L, source, URL);
        when(sourceReferenceRepository.findBySourceIn(List.of(source)))
                .thenReturn(List.of(sourceReference(source, "first", "f-1")));
        when(entryReferenceRepository.findByEntryIn(List.of(entry)))
                .thenReturn(List.of(entryReference(entry, "first", "42")));
        when(first.fetch(new ComicReadingSourceTarget(SOURCE_URL, "f-1"), new ComicReadingEntryTarget(URL, "42")))
                .thenReturn(Optional.of(details(URL)));
        when(first.supports(URL)).thenReturn(true);

        FetchOutcome outcome = registry.fetch(entry);

        assertThat(outcome).isEqualTo(found("first", details(URL)));
    }

    @Test
    void fetchAll_loadsTheReferencesOfAllEntriesWithOneQueryEach() {
        // Propósito: las referencias se cargan en lote, con una sola consulta para las fuentes y otra para
        // las entries, sea cual sea el número de entries; no una consulta por entry.
        //
        // Flujo:
        //  1. Dos entries de dos fuentes distintas, las dos vinculadas a "first".
        //  2. Se piden las dos; "first" responde que ninguna obra existe (el resultado no importa aquí).
        //  3. Se comprueba que findBySourceIn y findByEntryIn se llamaron exactamente una vez.
        ComicReadingSource sourceA = source(1L, SOURCE_URL);
        ComicReadingSource sourceB = source(2L, "https://other.example");
        ComicReadingEntry entryA = entry(10L, sourceA, URL + "/a");
        ComicReadingEntry entryB = entry(11L, sourceB, "https://other.example/b");
        when(sourceReferenceRepository.findBySourceIn(List.of(sourceA, sourceB))).thenReturn(
                List.of(sourceReference(sourceA, "first", "f-1"), sourceReference(sourceB, "first", "f-2")));
        when(first.fetch(any(), any())).thenReturn(Optional.empty());

        registry.fetchAll(List.of(entryA, entryB));

        verify(sourceReferenceRepository, times(1)).findBySourceIn(any());
        verify(entryReferenceRepository, times(1)).findByEntryIn(any());
    }

    @Test
    void fetchAll_buildsEachEntryTargetWithItsExternalIdInTheChosenProvider() {
        // Propósito: cada entry se pide con el id que tiene en el provider elegido para su fuente; los ids
        // que tiene en otros providers no se usan, y una entry sin id en ese provider se pide con null.
        //
        // Flujo:
        //  1. Dos entries de una fuente vinculada a "first". La primera tiene id "42" en "first" y "99" en
        //     "second"; la segunda no tiene ninguno.
        //  2. A "first" se le pide la primera con (URL, "42") y la segunda con (URL, null); a "second" no
        //     se le pide nada.
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntry hydrated = entry(10L, source, URL + "/a");
        ComicReadingEntry unhydrated = entry(11L, source, URL + "/b");
        ComicReadingSourceTarget sourceTarget = new ComicReadingSourceTarget(SOURCE_URL, "f-1");
        when(sourceReferenceRepository.findBySourceIn(List.of(source)))
                .thenReturn(List.of(sourceReference(source, "first", "f-1")));
        when(entryReferenceRepository.findByEntryIn(List.of(hydrated, unhydrated))).thenReturn(
                List.of(entryReference(hydrated, "first", "42"), entryReference(hydrated, "second", "99")));
        when(first.fetch(sourceTarget, new ComicReadingEntryTarget(URL + "/a", "42")))
                .thenReturn(Optional.of(details(URL + "/a")));
        when(first.fetch(sourceTarget, new ComicReadingEntryTarget(URL + "/b", null)))
                .thenReturn(Optional.of(details(URL + "/b")));
        when(first.supports(URL + "/a")).thenReturn(true);
        when(first.supports(URL + "/b")).thenReturn(true);

        List<FetchOutcome> outcomes = registry.fetchAll(List.of(hydrated, unhydrated));

        assertThat(outcomes).containsExactly(found("first", details(URL + "/a")),
                found("first", details(URL + "/b")));
        verify(second, never()).fetch(any(), any());
    }

    @Test
    void fetchAll_returnsOneOutcomePerEntryInOrderEvenWhenSomeFail() {
        // Propósito: un fallo no se propaga a las demás entries del lote: cada una recibe su propio
        // resultado, en el mismo orden en que se pidieron.
        //
        // Flujo, con tres entries:
        //  1. La primera es de una fuente vinculada y su provider responde: Found.
        //  2. La segunda es de esa misma fuente, pero su provider lanza una excepción: Failed.
        //  3. La tercera es de otra fuente sin vincular, y al preguntar por su host supports lanza una
        //     excepción: Failed. Esa elección fallida no afecta a las dos primeras.
        String otherSourceUrl = "https://other.example";
        ComicReadingSource linked = source(1L, SOURCE_URL);
        ComicReadingSource unlinked = source(2L, otherSourceUrl);
        ComicReadingEntry works = entry(10L, linked, URL + "/a");
        ComicReadingEntry providerFails = entry(11L, linked, URL + "/b");
        ComicReadingEntry selectionFails = entry(12L, unlinked, otherSourceUrl + "/c");
        ComicReadingSourceTarget sourceTarget = new ComicReadingSourceTarget(SOURCE_URL, "f-1");
        RuntimeException providerError = new RuntimeException("site is down");
        RuntimeException selectionError = new RuntimeException("service is down");
        when(sourceReferenceRepository.findBySourceIn(List.of(linked, unlinked)))
                .thenReturn(List.of(sourceReference(linked, "first", "f-1")));
        when(first.fetch(sourceTarget, new ComicReadingEntryTarget(URL + "/a", null)))
                .thenReturn(Optional.of(details(URL + "/a")));
        when(first.fetch(sourceTarget, new ComicReadingEntryTarget(URL + "/b", null))).thenThrow(providerError);
        when(first.supports(URL + "/a")).thenReturn(true);
        when(first.supports(otherSourceUrl)).thenThrow(selectionError);

        List<FetchOutcome> outcomes = registry.fetchAll(List.of(works, providerFails, selectionFails));

        assertThat(outcomes).containsExactly(found("first", details(URL + "/a")),
                new FetchOutcome.Failed(providerError), new FetchOutcome.Failed(selectionError));
    }

    @Test
    void fetch_keepsTheAuthorStatusAndSynopsisWhenItReplacesAUrlTheProviderDoesNotSupport() {
        // Propósito: cuando el provider devuelve una URL de un host que no sirve, el registro la sustituye por la
        // pedida; los datos que solo sirven para identificar la obra no se pierden por el camino.
        //
        // Flujo: el provider devuelve una URL que no soporta, con autor, estado y sinopsis. El resultado conserva
        // la URL pedida y esos tres datos.
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(URL, null);
        String otherHostUrl = "https://other.example.org/berserk";
        ComicReadingEntryDetails full = new ComicReadingEntryDetails("42", otherHostUrl, "Berserk", 374, null,
                "Kentaro Miura", ComicStatus.COMPLETED, "A mercenary and his sword");
        when(sourceReferenceRepository.findBySourceIn(List.of(source)))
                .thenReturn(List.of(sourceReference(source, "second", "s-9")));
        when(second.fetch(new ComicReadingSourceTarget(SOURCE_URL, "s-9"), target)).thenReturn(Optional.of(full));
        when(second.supports(otherHostUrl)).thenReturn(false);

        FetchOutcome outcome = registry.fetch(source, target);

        ComicReadingEntryDetails details = ((FetchOutcome.Found) outcome).entry().details();
        assertThat(details.url()).isEqualTo(URL);
        assertThat(details.author()).isEqualTo("Kentaro Miura");
        assertThat(details.status()).isEqualTo(ComicStatus.COMPLETED);
        assertThat(details.synopsis()).isEqualTo("A mercenary and his sword");
    }

    @Test
    void fetchFromProvider_usesTheRequestedProviderEvenWhenAnotherOneHasPriority() {
        // Propósito: el identificador de una obra solo vale para el provider que lo emitió, así que se pide
        // a ese provider aunque otro tenga más prioridad sobre la fuente.
        //
        // Flujo: la fuente está vinculada a "first" y a "second" (gana "first" al elegir por prioridad). Se
        // pide la obra a "second" con su identificador: se llama a "second" con la referencia que la fuente
        // tiene en él, y a "first" no se le pide nada.
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(null, "42");
        when(sourceReferenceRepository.findBySourceAndProviderKey(source, "second"))
                .thenReturn(Optional.of(sourceReference(source, "second", "s-9")));
        when(second.fetch(new ComicReadingSourceTarget(SOURCE_URL, "s-9"), target))
                .thenReturn(Optional.of(details(URL)));

        FetchOutcome outcome = registry.fetchFromProvider(source, "second", target);

        assertThat(outcome).isEqualTo(found("second", details(URL)));
        verify(first, never()).fetch(any(), any());
    }

    @Test
    void fetchFromProvider_fallsBackToTheHostWhenThatProviderHasNoReferenceForTheSource() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(null, "42");
        when(sourceReferenceRepository.findBySourceAndProviderKey(source, "second")).thenReturn(Optional.empty());
        when(second.supports(SOURCE_URL)).thenReturn(true);
        when(second.fetch(new ComicReadingSourceTarget(SOURCE_URL, null), target))
                .thenReturn(Optional.of(details(URL)));

        assertThat(registry.fetchFromProvider(source, "second", target)).isEqualTo(found("second", details(URL)));
    }

    @Test
    void fetchFromProvider_returnsNotSupportedWhenThatProviderDoesNotServeTheSource() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        when(sourceReferenceRepository.findBySourceAndProviderKey(source, "second")).thenReturn(Optional.empty());
        when(second.supports(SOURCE_URL)).thenReturn(false);

        FetchOutcome outcome = registry.fetchFromProvider(source, "second", new ComicReadingEntryTarget(null, "42"));

        assertThat(outcome).isEqualTo(new FetchOutcome.NotSupported());
        verify(first, never()).fetch(any(), any());
    }

    @Test
    void fetchFromProvider_returnsNotSupportedWhenNoProviderHasThatKey() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        when(sourceReferenceRepository.findBySourceAndProviderKey(source, "unknown")).thenReturn(Optional.empty());

        FetchOutcome outcome = registry.fetchFromProvider(source, "unknown", new ComicReadingEntryTarget(null, "42"));

        assertThat(outcome).isEqualTo(new FetchOutcome.NotSupported());
        verify(first, never()).fetch(any(), any());
        verify(second, never()).fetch(any(), any());
    }

    @Test
    void fetchFromProvider_returnsFailedWhenTheProviderThrows() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingEntryTarget target = new ComicReadingEntryTarget(null, "42");
        when(sourceReferenceRepository.findBySourceAndProviderKey(source, "second"))
                .thenReturn(Optional.of(sourceReference(source, "second", "s-9")));
        when(second.fetch(new ComicReadingSourceTarget(SOURCE_URL, "s-9"), target))
                .thenThrow(new IllegalStateException("site is down"));

        assertThat(registry.fetchFromProvider(source, "second", target)).isInstanceOf(FetchOutcome.Failed.class);
    }

    @Test
    void fetchAll_returnsNothingWithoutQueryingWhenThereAreNoEntries() {
        assertThat(registry.fetchAll(List.of())).isEmpty();

        verifyNoInteractions(sourceReferenceRepository, entryReferenceRepository);
    }

    @Test
    void search_usesTheProviderChosenForTheSourceAndReportsItsKey() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        ComicReadingSearchResult result = new ComicReadingSearchResult("7", "Berserk", null);
        when(sourceReferenceRepository.findBySourceIn(List.of(source)))
                .thenReturn(List.of(sourceReference(source, "second", "s-9")));
        when(second.search(new ComicReadingSourceTarget(SOURCE_URL, "s-9"), "berserk")).thenReturn(List.of(result));

        assertThat(registry.search(source, "berserk"))
                .isEqualTo(new ComicReadingSearchListing("second", List.of(result)));
    }

    @Test
    void search_throwsWhenNoProviderServesTheSource() {
        ComicReadingSource source = source(1L, SOURCE_URL);
        when(first.supports(SOURCE_URL)).thenReturn(false);
        when(second.supports(SOURCE_URL)).thenReturn(false);

        assertThatThrownBy(() -> registry.search(source, "berserk"))
                .isInstanceOf(ComicReadingSourceNotSupportedException.class)
                .extracting(exception -> ((ComicReadingSourceNotSupportedException) exception).getSourceId())
                .isEqualTo(1L);
    }

    @Test
    void discoverAll_returnsTheSourcesOfEveryProviderInOrder() {
        ComicReadingSourceDescriptor firstSource = new ComicReadingSourceDescriptor("a", "A", "https://a.com");
        ComicReadingSourceDescriptor secondSource = new ComicReadingSourceDescriptor("b", "B", "https://b.com");
        when(first.discoverSources()).thenReturn(List.of(firstSource));
        when(second.discoverSources()).thenReturn(List.of(secondSource));

        assertThat(registry.discoverAll()).containsExactly(
                new DiscoveryOutcome.Discovered("first", List.of(firstSource)),
                new DiscoveryOutcome.Discovered("second", List.of(secondSource)));
    }

    @Test
    void discoverAll_reportsAFailingProviderWithoutLosingTheOthers() {
        // Propósito: un provider que no puede listar sus sitios no impide conocer los de los demás.
        // Flujo:
        // 1. El primer provider lanza al listar sus sitios y el segundo responde con uno.
        // 2. discoverAll devuelve un Failed con la clave y la causa del primero y un Discovered del segundo.
        ComicReadingSourceDescriptor secondSource = new ComicReadingSourceDescriptor("b", "B", "https://b.com");
        IllegalStateException cause = new IllegalStateException("down");
        when(first.discoverSources()).thenThrow(cause);
        when(second.discoverSources()).thenReturn(List.of(secondSource));

        assertThat(registry.discoverAll()).containsExactly(new DiscoveryOutcome.Failed("first", cause),
                new DiscoveryOutcome.Discovered("second", List.of(secondSource)));
    }

}
