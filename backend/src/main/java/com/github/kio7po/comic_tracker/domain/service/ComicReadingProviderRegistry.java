package com.github.kio7po.comic_tracker.domain.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntryProviderReference;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSourceProviderReference;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotSupportedException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryProviderReferenceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceProviderReferenceRepository;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryTarget;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingProvider;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceTarget;

@Component
public class ComicReadingProviderRegistry {

    private final List<ComicReadingProvider> providers;
    private final ComicReadingSourceProviderReferenceRepository sourceProviderReferenceRepository;
    private final ComicReadingEntryProviderReferenceRepository entryProviderReferenceRepository;

    public ComicReadingProviderRegistry(List<ComicReadingProvider> providers,
            ComicReadingSourceProviderReferenceRepository sourceProviderReferenceRepository,
            ComicReadingEntryProviderReferenceRepository entryProviderReferenceRepository) {
        this.providers = providers;
        this.sourceProviderReferenceRepository = sourceProviderReferenceRepository;
        this.entryProviderReferenceRepository = entryProviderReferenceRepository;
    }

    /**
     * For a work that has no {@link ComicReadingEntry} yet.
     */
    public FetchOutcome fetch(ComicReadingSource source, ComicReadingEntryTarget target) {
        return prepareFetch(source, loadSourceExternalIds(source), provider -> target).get();
    }

    public FetchOutcome fetch(ComicReadingEntry entry) {
        return fetchAll(List.of(entry)).getFirst();
    }

    /**
     * @return one outcome per entry, in the same order as {@code entries}.
     */
    public List<FetchOutcome> fetchAll(List<ComicReadingEntry> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }

        // Todo el acceso a BD y la elección de provider ocurren aquí, en el hilo llamante: una sesión de
        // Hibernate no es thread-safe. Las tareas que devuelve prepareFetch solo llevan objetos planos.
        Map<Long, Map<String, String>> sourceExternalIds = loadSourceExternalIds(
                entries.stream().map(ComicReadingEntry::getSource).distinct().toList());
        Map<Long, Map<String, String>> entryExternalIds = loadEntryExternalIds(entries);

        List<Callable<FetchOutcome>> tasks = entries.stream()
                .<Callable<FetchOutcome>>map(entry -> {
                    Supplier<FetchOutcome> pendingFetch = prepareFetch(entry.getSource(),
                            sourceExternalIds.getOrDefault(entry.getSource().getId(), Map.of()),
                            provider -> new ComicReadingEntryTarget(entry.getUrl(),
                                    entryExternalIds.getOrDefault(entry.getId(), Map.of()).get(provider.key())));
                    return pendingFetch::get;
                })
                .toList();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // invokeAll solo vuelve cuando todas han terminado y devuelve los futures en el orden de las
            // tareas. Ninguna lanza excepciones de provider (se convierten en Failed), así que resultNow()
            // siempre tiene resultado.
            return executor.invokeAll(tasks).stream().map(Future::resultNow).toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.<FetchOutcome>nCopies(entries.size(), new FetchOutcome.Failed(e));
        }
    }

    /**
     * @return one outcome per provider, in provider order.
     */
    public List<DiscoveryOutcome> discoverAll() {
        return providers.stream().map(this::discover).toList();
    }

    /**
     * @throws ComicReadingSourceNotSupportedException if no provider serves the source.
     */
    public ComicReadingSearchListing search(ComicReadingSource source, String keywords) {
        ProviderChosenForSource chosen = chooseProviderFor(source, loadSourceExternalIds(source))
                .orElseThrow(() -> new ComicReadingSourceNotSupportedException(source.getId()));
        List<ComicReadingSearchResult> results = chosen.provider()
                .search(new ComicReadingSourceTarget(source.getUrl(), chosen.sourceExternalId()), keywords);
        return new ComicReadingSearchListing(chosen.provider().key(), results);
    }

    private DiscoveryOutcome discover(ComicReadingProvider provider) {
        try {
            return new DiscoveryOutcome.Discovered(provider.key(), provider.discoverSources());
        } catch (RuntimeException e) {
            return new DiscoveryOutcome.Failed(provider.key(), e);
        }
    }

    // Elige el provider y construye los targets ya, en el hilo llamante; lo que se devuelve es solo la
    // llamada pendiente. Si la elección falla (p. ej. supports lanza), la tarea es ese fallo.
    private Supplier<FetchOutcome> prepareFetch(ComicReadingSource source,
            Map<String, String> sourceExternalIdByProviderKey,
            Function<ComicReadingProvider, ComicReadingEntryTarget> buildEntryTarget) {
        try {
            Optional<ProviderChosenForSource> chosen = chooseProviderFor(source, sourceExternalIdByProviderKey);
            if (chosen.isEmpty()) {
                return FetchOutcome.NotSupported::new;
            }
            ComicReadingProvider provider = chosen.get().provider();
            ComicReadingSourceTarget sourceTarget = new ComicReadingSourceTarget(source.getUrl(),
                    chosen.get().sourceExternalId());
            ComicReadingEntryTarget entryTarget = buildEntryTarget.apply(provider);
            return () -> callProvider(provider, sourceTarget, entryTarget);
        } catch (RuntimeException e) {
            return () -> new FetchOutcome.Failed(e);
        }
    }

    // Manda la referencia de la fuente (en el orden de los providers); solo si no tiene ninguna se recurre
    // al host.
    private Optional<ProviderChosenForSource> chooseProviderFor(ComicReadingSource source,
            Map<String, String> sourceExternalIdByProviderKey) {
        for (ComicReadingProvider provider : providers) {
            String sourceExternalId = sourceExternalIdByProviderKey.get(provider.key());
            if (sourceExternalId != null) {
                return Optional.of(new ProviderChosenForSource(provider, sourceExternalId));
            }
        }
        for (ComicReadingProvider provider : providers) {
            if (provider.supports(source.getUrl())) {
                return Optional.of(new ProviderChosenForSource(provider, null));
            }
        }
        return Optional.empty();
    }

    private FetchOutcome callProvider(ComicReadingProvider provider, ComicReadingSourceTarget source,
            ComicReadingEntryTarget entry) {
        try {
            return provider.fetch(source, entry)
                    .<FetchOutcome>map(details -> new FetchOutcome.Found(
                            new FetchedEntry(provider.key(), withSupportedUrl(provider, details, entry.url()))))
                    .orElseGet(FetchOutcome.NotFound::new);
        } catch (RuntimeException e) {
            return new FetchOutcome.Failed(e);
        }
    }

    // Un provider podría devolver una URL de un host que no sirve; en ese caso se conserva la que ya había.
    // Si no se puede comprobar, se trata igual: conservar la pedida es lo prudente.
    private static ComicReadingEntryDetails withSupportedUrl(ComicReadingProvider provider,
            ComicReadingEntryDetails details, @Nullable String fallbackUrl) {
        if (fallbackUrl == null || supportsSafely(provider, details.url())) {
            return details;
        }
        return new ComicReadingEntryDetails(details.externalId(), fallbackUrl, details.title(),
                details.availableChapters(), details.latestChapterAt());
    }

    private static boolean supportsSafely(ComicReadingProvider provider, String url) {
        try {
            return provider.supports(url);
        } catch (RuntimeException e) {
            return false;
        }
    }

    // El id que la fuente tiene en cada provider que la conoce, por providerKey.
    private Map<String, String> loadSourceExternalIds(ComicReadingSource source) {
        return loadSourceExternalIds(List.of(source)).getOrDefault(source.getId(), Map.of());
    }

    // idFuente -> (providerKey -> id de la fuente en ese provider)
    private Map<Long, Map<String, String>> loadSourceExternalIds(List<ComicReadingSource> sources) {
        return sourceProviderReferenceRepository.findBySourceIn(sources).stream()
                .collect(Collectors.groupingBy(reference -> reference.getSource().getId(),
                        Collectors.toMap(ComicReadingSourceProviderReference::getProviderKey,
                                ComicReadingSourceProviderReference::getExternalId)));
    }

    // idEntry -> (providerKey -> id de la entry en ese provider)
    private Map<Long, Map<String, String>> loadEntryExternalIds(List<ComicReadingEntry> entries) {
        return entryProviderReferenceRepository.findByEntryIn(entries).stream()
                .collect(Collectors.groupingBy(reference -> reference.getEntry().getId(),
                        Collectors.toMap(ComicReadingEntryProviderReference::getProviderKey,
                                ComicReadingEntryProviderReference::getExternalId)));
    }

    private record ProviderChosenForSource(ComicReadingProvider provider, @Nullable String sourceExternalId) {
    }

}
