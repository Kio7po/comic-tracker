package com.github.kio7po.comic_tracker.domain.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.common.Slugifier;
import com.github.kio7po.comic_tracker.domain.common.UrlNormalizer;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSourceProviderReference;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.ComicReadingSourceStatus;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceProviderReferenceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncReport.SkipReason;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncReport.SkippedSource;
import com.github.kio7po.comic_tracker.domain.service.ComicReadingSourceSyncReport.SyncedSource;

@Service
public class ComicReadingSourceSyncService {

    private final ComicReadingProviderRegistry providerRegistry;
    private final ComicReadingSourceRepository sourceRepository;
    private final ComicReadingSourceProviderReferenceRepository referenceRepository;
    private final ComicReadingSourceIconResolverRegistry iconResolverRegistry;
    private final UserService userService;

    public ComicReadingSourceSyncService(ComicReadingProviderRegistry providerRegistry,
            ComicReadingSourceRepository sourceRepository,
            ComicReadingSourceProviderReferenceRepository referenceRepository,
            ComicReadingSourceIconResolverRegistry iconResolverRegistry, UserService userService) {
        this.providerRegistry = providerRegistry;
        this.sourceRepository = sourceRepository;
        this.referenceRepository = referenceRepository;
        this.iconResolverRegistry = iconResolverRegistry;
        this.userService = userService;
    }

    /**
     * Brings the sources in line with what every provider reports. The sources it creates are approved
     * and attributed to {@code adminId}; the status of the ones that already exist is never changed.
     */
    @Transactional
    public ComicReadingSourceSyncReport sync(Long adminId) {
        Run run = new Run(userService.findById(adminId), sourceRepository.findAll());

        // TODO: discoverAll() llama a los providers (red) con la transacción abierta, así que retiene una
        // conexión de BD mientras espera. Aceptado: lo lanza un admin a mano de vez en cuando. Se evitaría
        // haciéndolo antes de abrir la transacción (p. ej. con un TransactionTemplate para la parte que escribe).
        for (DiscoveryOutcome outcome : providerRegistry.discoverAll()) {
            switch (outcome) {
                case DiscoveryOutcome.Discovered discovered -> syncProvider(discovered, run);
                case DiscoveryOutcome.Failed failed -> run.failedProviders.add(failed);
            }
        }

        return new ComicReadingSourceSyncReport(run.created, run.linked, run.updated, run.skipped,
                run.failedProviders);
    }

    private void syncProvider(DiscoveryOutcome.Discovered discovered, Run run) {
        String providerKey = discovered.providerKey();
        ProviderReferences references = new ProviderReferences(referenceRepository.findByProviderKey(providerKey));
        // Orígenes ya vistos en este provider: dos descriptores con el mismo origen (p. ej. una
        // extensión con un source por idioma) no caben, solo hay una referencia por fuente y provider.
        // TODO: los descriptores que se saltan por esto (DUPLICATE_URL) se pierden: solo se puede buscar
        // con el primero. Admitir varias referencias por fuente y provider lo resolvería, pero obliga a
        // cambiar la restricción UNIQUE (source_id, provider_key) y a decidir cuál usar en cada fetch.
        Set<String> seenOrigins = new HashSet<>();

        for (ComicReadingSourceDescriptor descriptor : discovered.sources()) {
            if (descriptor.url().isBlank()) {
                run.skip(providerKey, descriptor, SkipReason.NO_URL);
                continue;
            }

            String origin = originOf(descriptor.url());
            if (origin == null) {
                run.skip(providerKey, descriptor, SkipReason.INVALID_URL);
            } else if (!seenOrigins.add(origin)) {
                run.skip(providerKey, descriptor, SkipReason.DUPLICATE_URL);
            } else {
                syncSource(providerKey, descriptor, origin, references, run);
            }
        }
    }

    private void syncSource(String providerKey, ComicReadingSourceDescriptor descriptor, String origin,
            ProviderReferences references, Run run) {
        ComicReadingSourceProviderReference linkedReference = references.byExternalId.get(descriptor.externalId());
        ComicReadingSource existing = run.sourcesByOrigin.get(origin);

        if (linkedReference != null) {
            updateOrigin(providerKey, descriptor, linkedReference.getSource(), origin, run);
        } else if (existing != null) {
            link(providerKey, descriptor, existing, references, run);
        } else {
            create(providerKey, descriptor, origin, references, run);
        }
    }

    private void updateOrigin(String providerKey, ComicReadingSourceDescriptor descriptor,
            ComicReadingSource source, String origin, Run run) {
        if (origin.equals(source.getUrl())) {
            return;
        }
        if (run.sourcesByOrigin.containsKey(origin)) {
            run.skip(providerKey, descriptor, SkipReason.URL_IN_USE);
            return;
        }

        run.sourcesByOrigin.remove(source.getUrl());
        source.setUrl(origin);
        run.sourcesByOrigin.put(origin, sourceRepository.save(source));
        run.updated.add(new SyncedSource(providerKey, source));
    }

    private void link(String providerKey, ComicReadingSourceDescriptor descriptor, ComicReadingSource source,
            ProviderReferences references, Run run) {
        // TODO: una fuente PENDING que se vincula aquí sigue PENDING, aunque la haya instalado un admin en el
        // provider. Decidir si vincularla debería aprobarla; de momento no se toca el estado de las existentes.

        // Si la fuente ya tenía referencia de este provider con otro id externo, el provider es la autoridad
        // sobre sus ids: se sustituye para no dejarla rota.
        ComicReadingSourceProviderReference reference = references.bySourceId.get(source.getId());
        if (reference == null) {
            reference = new ComicReadingSourceProviderReference();
            reference.setSource(source);
            reference.setProviderKey(providerKey);
        } else {
            references.byExternalId.remove(reference.getExternalId());
        }
        reference.setExternalId(descriptor.externalId());

        references.add(referenceRepository.save(reference));
        run.linked.add(new SyncedSource(providerKey, source));
    }

    private void create(String providerKey, ComicReadingSourceDescriptor descriptor, String origin,
            ProviderReferences references, Run run) {
        ComicReadingSource source = new ComicReadingSource();
        source.setSlug(run.uniqueSlug(descriptor.name()));
        source.setName(descriptor.name());
        source.setUrl(origin);
        source.setStatus(ComicReadingSourceStatus.APPROVED);
        source.setContributedBy(run.admin);
        source.setReviewedBy(run.admin);
        source.setReviewedAt(Instant.now());
        source.setIconUrl(iconResolverRegistry.resolveIconUrl(source).orElse(null));
        source = sourceRepository.save(source);

        ComicReadingSourceProviderReference reference = new ComicReadingSourceProviderReference();
        reference.setSource(source);
        reference.setProviderKey(providerKey);
        reference.setExternalId(descriptor.externalId());
        references.add(referenceRepository.save(reference));

        run.sourcesByOrigin.put(origin, source);
        run.created.add(new SyncedSource(providerKey, source));
    }

    // null si de la URL no se puede sacar un origen.
    private static @Nullable String originOf(String url) {
        try {
            return UrlNormalizer.normalizeOrigin(url);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // Las referencias del provider indexadas por los dos lados por los que se buscan: el id que da el
    // provider y la fuente. Se mantienen al día según se crean o cambian durante el sync.
    private static final class ProviderReferences {
        private final Map<String, ComicReadingSourceProviderReference> byExternalId;
        private final Map<Long, ComicReadingSourceProviderReference> bySourceId;

        private ProviderReferences(List<ComicReadingSourceProviderReference> references) {
            this.byExternalId = references.stream()
                    .collect(Collectors.toMap(ComicReadingSourceProviderReference::getExternalId, Function.identity()));
            this.bySourceId = references.stream()
                    .collect(Collectors.toMap(reference -> reference.getSource().getId(), Function.identity()));
        }

        private void add(ComicReadingSourceProviderReference reference) {
            byExternalId.put(reference.getExternalId(), reference);
            bySourceId.put(reference.getSource().getId(), reference);
        }
    }

    // Estado de una ejecución: las fuentes ya existentes en memoria, para no hacer una consulta por
    // descriptor, y el informe que se va rellenando.
    private static final class Run {
        private final User admin;
        private final Map<String, ComicReadingSource> sourcesByOrigin = new HashMap<>();
        private final Set<String> slugs = new HashSet<>();
        private final List<SyncedSource> created = new ArrayList<>();
        private final List<SyncedSource> linked = new ArrayList<>();
        private final List<SyncedSource> updated = new ArrayList<>();
        private final List<SkippedSource> skipped = new ArrayList<>();
        private final List<DiscoveryOutcome.Failed> failedProviders = new ArrayList<>();

        private Run(User admin, List<ComicReadingSource> existingSources) {
            this.admin = admin;
            for (ComicReadingSource source : existingSources) {
                sourcesByOrigin.putIfAbsent(source.getUrl(), source);
                slugs.add(source.getSlug());
            }
        }

        private void skip(String providerKey, ComicReadingSourceDescriptor descriptor, SkipReason reason) {
            skipped.add(new SkippedSource(providerKey, descriptor, reason));
        }

        private String uniqueSlug(String name) {
            String base = Slugifier.slugify(name);
            String slug = base;
            for (int suffix = 2; !slugs.add(slug); suffix++) {
                slug = base + "-" + suffix;
            }
            return slug;
        }
    }

}
