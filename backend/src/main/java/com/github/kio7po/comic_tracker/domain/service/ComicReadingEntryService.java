package com.github.kio7po.comic_tracker.domain.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.common.Slugifier;
import com.github.kio7po.comic_tracker.domain.common.UrlNormalizer;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntryProviderReference;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntrySortField;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingEntryStatus;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundInReadingSourceException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryAlreadyReviewedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingEntryNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingProviderFailedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotApprovedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotSupportedException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingEntryException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingSourceException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryProviderReferenceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryTarget;

@Service
public class ComicReadingEntryService {

    private final ComicReadingEntryRepository comicReadingEntryRepository;
    private final ComicReadingEntryProviderReferenceRepository entryProviderReferenceRepository;
    private final ComicReadingSourceRepository comicReadingSourceRepository;
    private final ComicRepository comicRepository;
    private final ComicReadingSourceIconResolverRegistry iconResolverRegistry;
    private final ComicReadingProviderRegistry readingProviderRegistry;
    private final UserService userService;
    private final Duration ttl;
    private final Duration retryBackoff;

    public ComicReadingEntryService(ComicReadingEntryRepository comicReadingEntryRepository,
            ComicReadingEntryProviderReferenceRepository entryProviderReferenceRepository,
            ComicReadingSourceRepository comicReadingSourceRepository, ComicRepository comicRepository,
            ComicReadingSourceIconResolverRegistry iconResolverRegistry,
            ComicReadingProviderRegistry readingProviderRegistry, UserService userService,
            @Value("${comic.reading-entry.ttl}") Duration ttl,
            @Value("${comic.reading-entry.retry-backoff}") Duration retryBackoff) {
        this.comicReadingEntryRepository = comicReadingEntryRepository;
        this.entryProviderReferenceRepository = entryProviderReferenceRepository;
        this.comicReadingSourceRepository = comicReadingSourceRepository;
        this.comicRepository = comicRepository;
        this.iconResolverRegistry = iconResolverRegistry;
        this.readingProviderRegistry = readingProviderRegistry;
        this.userService = userService;
        this.ttl = ttl;
        this.retryBackoff = retryBackoff;
    }

    /**
     * @param url must already be a well-formed URL.
     * @param locale must already be a well-formed BCP 47 tag.
     */
    @Transactional
    public ComicReadingEntry submit(Long comicId, Long sourceId, String url, String locale, Long contributorId) {
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));
        ComicReadingSource source = comicReadingSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ComicReadingSourceNotFoundException(sourceId));
        User contributor = userService.findById(contributorId);

        return createEntry(comic, source, UrlNormalizer.normalize(url), locale, contributor);
    }

    /**
     * For a site not registered as a {@link ComicReadingSource} yet.
     *
     * @param sourceUrl must already be a well-formed URL.
     * @param url must already be a well-formed URL.
     * @param locale must already be a well-formed BCP 47 tag.
     * @throws DuplicateComicReadingSourceException if {@code sourceUrl} is already registered.
     */
    @Transactional
    public ComicReadingEntry submitWithNewSource(Long comicId, String sourceName, String sourceUrl, String url,
            String locale, Long contributorId) {
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));
        User contributor = userService.findById(contributorId);
        ComicReadingSource source = createSource(sourceName, UrlNormalizer.normalizeOrigin(sourceUrl), contributor);

        return createEntry(comic, source, UrlNormalizer.normalize(url), locale, contributor);
    }

    /**
     * Creates the entry for a work picked from the search results of a source. The work is requested by its
     * external id from the provider that ran the search, since that id is only valid for it, and the entry is
     * only created if that provider hydrates it.
     *
     * @param locale must already be a well-formed BCP 47 tag.
     * @throws ComicReadingSourceNotApprovedException if the source isn't approved.
     * @throws ComicReadingSourceNotSupportedException if that provider doesn't serve the source.
     * @throws ComicNotFoundInReadingSourceException if the provider answered that the work doesn't exist.
     * @throws ComicReadingProviderFailedException if the provider couldn't answer.
     * @throws DuplicateComicReadingEntryException if the comic already has an entry for that work in the source.
     */
    @Transactional
    public ComicReadingEntry submitFromSearchResult(Long comicId, Long sourceId, String providerKey,
            String externalId, String locale, Long contributorId) {
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));
        ComicReadingSource source = comicReadingSourceRepository.findById(sourceId)
                .orElseThrow(() -> new ComicReadingSourceNotFoundException(sourceId));
        if (source.getStatus() != ComicReadingSourceStatus.APPROVED) {
            throw new ComicReadingSourceNotApprovedException(sourceId, source.getStatus());
        }
        User contributor = userService.findById(contributorId);

        // Igual que en createEntry, el provider se llama con la transacción abierta (ver el TODO de allí).
        FetchedEntry fetched = hydratedOrFailed(readingProviderRegistry.fetchFromProvider(source, providerKey,
                new ComicReadingEntryTarget(null, externalId)), sourceId, providerKey, externalId);

        return saveEntry(comic, source, fetched.details().url(), locale, contributor, Optional.of(fetched));
    }

    private ComicReadingSource createSource(String name, String url, User contributor) {
        comicReadingSourceRepository.findByUrl(url).ifPresent(existing -> {
            throw new DuplicateComicReadingSourceException(url, existing.getId());
        });

        // TODO: slug is derived from `name`, not from `url` (the identity check above). Two
        // genuinely different sources whose names happen to slugify the same would collide on
        // the DB's UNIQUE(slug) constraint. Narrow edge case, not solved now.
        ComicReadingSource source = new ComicReadingSource();
        source.setSlug(Slugifier.slugify(name));
        source.setName(name);
        source.setUrl(url);
        source.setContributedBy(contributor);
        source.setIconUrl(iconResolverRegistry.resolveIconUrl(source).orElse(null));

        return comicReadingSourceRepository.save(source);
    }

    private ComicReadingEntry createEntry(Comic comic, ComicReadingSource source, String url, String locale,
            User contributor) {
        // TODO: submit y submitWithNewSource llaman aquí al provider (red) con la transacción abierta, así que
        // retienen una conexión de BD mientras esperan al sitio externo. Aceptado de momento: reordenar qué va
        // dentro y fuera de la transacción es costoso, porque el resultado alimenta decisiones que ya leen de BD.
        Optional<FetchedEntry> fetched = hydratedOrRejected(
                readingProviderRegistry.fetch(source, new ComicReadingEntryTarget(url, null)), url);

        return saveEntry(comic, source, url, locale, contributor, fetched);
    }

    private ComicReadingEntry saveEntry(Comic comic, ComicReadingSource source, String url, String locale,
            User contributor, Optional<FetchedEntry> fetched) {
        Optional<ComicReadingEntryDetails> details = fetched.map(FetchedEntry::details);
        String canonicalUrl = details.map(ComicReadingEntryDetails::url).orElse(url);
        if (alreadyExists(comic, source, canonicalUrl, fetched)) {
            throw new DuplicateComicReadingEntryException(comic.getId(), source.getId(), canonicalUrl);
        }

        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setComic(comic);
        entry.setSource(source);
        entry.setUrl(canonicalUrl);
        entry.setLocale(locale);
        entry.setContributedBy(contributor);
        details.ifPresent(fetchedDetails -> {
            copyHydratedFields(entry, fetchedDetails);
            entry.setLastFetchedAt(Instant.now());
        });

        ComicReadingEntry saved = comicReadingEntryRepository.save(entry);
        fetched.ifPresent(fetchedEntry -> saveProviderReference(saved, fetchedEntry));
        return saved;
    }

    // Solo un provider que respondió y no encontró la obra rechaza el envío. Sin provider para el sitio, o
    // con uno que falló, la entry se guarda sin hidratar: una caída no debe bloquear las contribuciones.
    private Optional<FetchedEntry> hydratedOrRejected(FetchOutcome outcome, String url) {
        return switch (outcome) {
            case FetchOutcome.Found(FetchedEntry found) -> Optional.of(found);
            case FetchOutcome.NotFound notFound -> throw new ComicNotFoundInReadingSourceException(url);
            // TODO: logs
            case FetchOutcome.Failed failed -> Optional.empty();
            case FetchOutcome.NotSupported notSupported -> Optional.empty();
        };
    }

    // A diferencia de hydratedOrRejected, aquí la hidratación es el propósito del envío: sin ella no hay entry,
    // así que cualquier respuesta que no sea Found es un error.
    private FetchedEntry hydratedOrFailed(FetchOutcome outcome, Long sourceId, String providerKey,
            String externalId) {
        return switch (outcome) {
            case FetchOutcome.Found(FetchedEntry found) -> found;
            case FetchOutcome.NotFound notFound ->
                throw new ComicNotFoundInReadingSourceException(sourceId, externalId);
            case FetchOutcome.Failed(Exception cause) ->
                throw new ComicReadingProviderFailedException(sourceId, providerKey, cause);
            case FetchOutcome.NotSupported notSupported -> throw new ComicReadingSourceNotSupportedException(sourceId);
        };
    }

    // The URL alone isn't enough: the same work can be reachable through more than one URL, which only
    // shows once the provider has said what work it is.
    private boolean alreadyExists(Comic comic, ComicReadingSource source, String url,
            Optional<FetchedEntry> fetched) {
        return comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, url).isPresent()
                || fetched.filter(fetchedEntry -> fetchedEntry.details().externalId() != null)
                        .filter(fetchedEntry -> entryProviderReferenceRepository
                                .existsByEntryComicAndEntrySourceAndProviderKeyAndExternalId(comic, source,
                                        fetchedEntry.providerKey(), fetchedEntry.details().externalId()))
                        .isPresent();
    }

    // No todos los sitios dan un identificador estable de la obra; sin él no hay referencia que guardar.
    private void saveProviderReference(ComicReadingEntry entry, FetchedEntry fetched) {
        String externalId = fetched.details().externalId();
        if (externalId == null) {
            return;
        }
        ComicReadingEntryProviderReference reference = new ComicReadingEntryProviderReference();
        reference.setEntry(entry);
        reference.setProviderKey(fetched.providerKey());
        reference.setExternalId(externalId);
        entryProviderReferenceRepository.save(reference);
    }

    public List<ComicReadingEntry> findByComic(Long comicId, @Nullable ComicReadingEntryStatus status) {
        Comic comic = comicRepository.findById(comicId).orElseThrow(() -> new ComicNotFoundException(comicId));

        List<ComicReadingEntry> entries = status == null ? comicReadingEntryRepository.findByComic(comic)
                : comicReadingEntryRepository.findByComicAndStatus(comic, status);

        refreshStaleEntries(entries);

        return entries;
    }

    // Las llamadas a los providers (en paralelo) las hace el registro sin que ninguna entity cruce a otro
    // hilo. Aquí, de vuelta en el hilo llamante, se aplica cada resultado a su entity y se persiste todo de
    // una vez con saveAll en lugar de un save() por entry.
    private void refreshStaleEntries(List<ComicReadingEntry> entries) {
        Instant now = Instant.now();
        List<ComicReadingEntry> stale = entries.stream().filter(entry -> needsRefresh(entry, now)).toList();
        if (stale.isEmpty()) {
            return;
        }

        List<FetchOutcome> outcomes = readingProviderRegistry.fetchAll(stale);

        // fetchAll devuelve un resultado por entry, en el mismo orden, así que se emparejan por índice.
        List<ComicReadingEntry> updated = new ArrayList<>(stale.size());
        Set<UrlKey> claimedUrls = new HashSet<>();
        for (int i = 0; i < stale.size(); i++) {
            updated.add(applyOutcome(stale.get(i), outcomes.get(i), now, claimedUrls));
        }
        comicReadingEntryRepository.saveAll(updated);
        saveProviderReferences(stale, outcomes);
    }

    private boolean needsRefresh(ComicReadingEntry entry, Instant now) {
        return entry.getStatus() != ComicReadingEntryStatus.REJECTED
                && (entry.getLastFetchedAt() == null || now.isAfter(entry.getLastFetchedAt().plus(ttl)));
    }

    private ComicReadingEntry applyOutcome(ComicReadingEntry entry, FetchOutcome outcome, Instant now,
            Set<UrlKey> claimedUrls) {
        switch (outcome) {
            case FetchOutcome.Found(FetchedEntry found) -> {
                applyFetchedDetails(entry, found.details(), claimedUrls);
                entry.setLastFetchedAt(now);
            }
            // Sin datos nuevos (la obra ya no existe, o ningún provider sirve la fuente): se conservan los
            // que hay y no se vuelve a preguntar hasta el siguiente TTL.
            case FetchOutcome.NotFound notFound -> entry.setLastFetchedAt(now);
            case FetchOutcome.NotSupported notSupported -> entry.setLastFetchedAt(now);
            // TODO: logs
            case FetchOutcome.Failed failed -> entry.setLastFetchedAt(now.minus(ttl).plus(retryBackoff));
        }
        return entry;
    }

    private void applyFetchedDetails(ComicReadingEntry entry, ComicReadingEntryDetails details,
            Set<UrlKey> claimedUrls) {
        copyHydratedFields(entry, details);
        if (canMoveToUrl(entry, details.url(), claimedUrls)) {
            entry.setUrl(details.url());
        }
    }

    // (comic, source, url) is unique. If another entry already holds the URL this one would move to (or
    // an earlier entry of this same batch is moving to it), keep the old URL instead: it still resolves
    // through its provider reference, whereas a violation would make saveAll fail for every entry of the comic.
    private boolean canMoveToUrl(ComicReadingEntry entry, String newUrl, Set<UrlKey> claimedUrls) {
        if (newUrl.equals(entry.getUrl())) {
            return false;
        }
        UrlKey key = new UrlKey(entry.getSource().getId(), newUrl);
        boolean taken = claimedUrls.contains(key) || comicReadingEntryRepository
                .findByComicAndSourceAndUrl(entry.getComic(), entry.getSource(), newUrl).isPresent();
        if (taken) {
            return false;
        }
        claimedUrls.add(key);
        return true;
    }

    private void copyHydratedFields(ComicReadingEntry entry, ComicReadingEntryDetails details) {
        entry.setTitle(details.title());
        entry.setAvailableChapters(details.availableChapters());
        entry.setLatestChapterAt(details.latestChapterAt());
    }

    // Tras un refresco, guarda en cada entry el identificador que su provider le ha dado a la obra: crea la
    // referencia si la entry aún no tenía una de ese provider, o actualiza el identificador si ya la tenía.
    // Se hace en lote (una consulta para leer las existentes y un único saveAll), no entry a entry.
    private void saveProviderReferences(List<ComicReadingEntry> entries, List<FetchOutcome> outcomes) {
        // Solo interesan las entries cuyo refresco devolvió datos y un identificador. `entries` y
        // `outcomes` van en el mismo orden, así que se emparejan por índice; `refreshed` y `fetched`
        // quedan alineadas entre sí, la posición i de una corresponde a la posición i de la otra.
        List<ComicReadingEntry> refreshed = new ArrayList<>();
        List<FetchedEntry> fetched = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            if (outcomes.get(i) instanceof FetchOutcome.Found(FetchedEntry found)
                    && found.details().externalId() != null) {
                refreshed.add(entries.get(i));
                fetched.add(found);
            }
        }
        if (refreshed.isEmpty()) {
            return;
        }

        // Las referencias que esas entries ya tienen, indexadas por (entry, provider) para encontrar en
        // el paso siguiente la que toca actualizar. toMap recibe cómo calcular la clave y cómo calcular el
        // valor; Function.identity() devuelve el propio elemento, así que el valor es la referencia tal cual
        // (equivale a `reference -> reference`).
        Map<ReferenceKey, ComicReadingEntryProviderReference> existing = entryProviderReferenceRepository
                .findByEntryIn(refreshed).stream()
                .collect(Collectors.toMap(
                        reference -> new ReferenceKey(reference.getEntry().getId(), reference.getProviderKey()),
                        Function.identity()));

        // Por cada entry: la referencia que ya tiene con el provider que respondió, o una nueva si no
        // la tenía (p. ej. la primera vez que se hidrata, o si la fuente ha cambiado de provider).
        // Después se fija el identificador que acaba de devolver ese provider.
        List<ComicReadingEntryProviderReference> references = new ArrayList<>(refreshed.size());
        for (int i = 0; i < refreshed.size(); i++) {
            ComicReadingEntry entry = refreshed.get(i);
            FetchedEntry fetchedEntry = fetched.get(i);
            ComicReadingEntryProviderReference reference = existing
                    .get(new ReferenceKey(entry.getId(), fetchedEntry.providerKey()));
            if (reference == null) {
                reference = new ComicReadingEntryProviderReference();
                reference.setEntry(entry);
                reference.setProviderKey(fetchedEntry.providerKey());
            }
            reference.setExternalId(fetchedEntry.details().externalId());
            references.add(reference);
        }
        entryProviderReferenceRepository.saveAll(references);
    }

    private record UrlKey(Long sourceId, String url) {
    }

    private record ReferenceKey(Long entryId, String providerKey) {
    }

    public Page<ComicReadingEntry> findByStatusIn(List<ComicReadingEntryStatus> statuses,
            ComicReadingEntrySortField sortBy, SortDirection direction, int limit, int offset) {
        return comicReadingEntryRepository.findByStatusIn(statuses, sortBy, direction, limit, offset);
    }

    @Transactional
    public ComicReadingEntry approve(Long entryId, Long reviewerId) {
        ComicReadingEntry entry = findPendingEntry(entryId);

        ComicReadingSource source = entry.getSource();
        if (source.getStatus() != ComicReadingSourceStatus.APPROVED) {
            throw new ComicReadingSourceNotApprovedException(source.getId(), source.getStatus());
        }

        return resolve(entry, userService.findById(reviewerId), ComicReadingEntryStatus.APPROVED);
    }

    @Transactional
    public ComicReadingEntry reject(Long entryId, Long reviewerId) {
        return resolve(findPendingEntry(entryId), userService.findById(reviewerId), ComicReadingEntryStatus.REJECTED);
    }

    private ComicReadingEntry findPendingEntry(Long entryId) {
        ComicReadingEntry entry = comicReadingEntryRepository.findById(entryId)
                .orElseThrow(() -> new ComicReadingEntryNotFoundException(entryId));

        if (entry.getStatus() != ComicReadingEntryStatus.PENDING) {
            throw new ComicReadingEntryAlreadyReviewedException(entryId, entry.getStatus());
        }

        return entry;
    }

    private ComicReadingEntry resolve(ComicReadingEntry entry, User reviewer, ComicReadingEntryStatus resolution) {
        entry.setStatus(resolution);
        entry.setReviewedBy(reviewer);
        entry.setReviewedAt(Instant.now());

        return comicReadingEntryRepository.save(entry);
    }

}
