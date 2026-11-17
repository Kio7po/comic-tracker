package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class ComicReadingSourceSyncServiceTest {

    private static final Long ADMIN_ID = 7L;
    private static final String PROVIDER = "suwayomi";

    @Mock
    private ComicReadingProviderRegistry providerRegistry;
    @Mock
    private ComicReadingSourceRepository sourceRepository;
    @Mock
    private ComicReadingSourceProviderReferenceRepository referenceRepository;
    @Mock
    private ComicReadingSourceIconResolverRegistry iconResolverRegistry;
    @Mock
    private UserService userService;

    // Recoge las referencias que el servicio guarda, para comprobar sus campos.
    @Captor
    private ArgumentCaptor<ComicReadingSourceProviderReference> savedReference;

    private final User admin = new User();
    private long nextSourceId = 100;

    private ComicReadingSourceSyncService service;

    @BeforeEach
    void setUp() {
        lenient().when(userService.findById(ADMIN_ID)).thenReturn(admin);
        lenient().when(iconResolverRegistry.resolveIconUrl(any())).thenReturn(Optional.empty());
        // Como la BD: una fuente nueva recibe id al guardarse.
        lenient().when(sourceRepository.save(any())).thenAnswer(invocation -> {
            ComicReadingSource source = invocation.getArgument(0);
            if (source.getId() == null) {
                source.setId(nextSourceId++);
            }
            return source;
        });
        lenient().when(referenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ComicReadingSourceSyncService(providerRegistry, sourceRepository, referenceRepository,
                iconResolverRegistry, userService);
    }

    private static ComicReadingSourceDescriptor descriptor(String externalId, String name, String url) {
        return new ComicReadingSourceDescriptor(externalId, name, url);
    }

    private static ComicReadingSource source(Long id, String slug, String name, String url,
            ComicReadingSourceStatus status) {
        ComicReadingSource source = new ComicReadingSource();
        source.setId(id);
        source.setSlug(slug);
        source.setName(name);
        source.setUrl(url);
        source.setStatus(status);
        return source;
    }

    private static ComicReadingSource source(Long id, String slug, String url) {
        return source(id, slug, slug, url, ComicReadingSourceStatus.APPROVED);
    }

    private static ComicReadingSourceProviderReference reference(ComicReadingSource source, String providerKey,
            String externalId) {
        ComicReadingSourceProviderReference reference = new ComicReadingSourceProviderReference();
        reference.setSource(source);
        reference.setProviderKey(providerKey);
        reference.setExternalId(externalId);
        return reference;
    }

    private void providerReports(String providerKey, ComicReadingSourceDescriptor... descriptors) {
        when(providerRegistry.discoverAll())
                .thenReturn(List.of(new DiscoveryOutcome.Discovered(providerKey, List.of(descriptors))));
    }

    @Test
    void sync_createsAnApprovedSourceForADescriptorNobodyKnows() {
        providerReports(PROVIDER, descriptor("1", "MangaDex", "https://mangadex.org/titles?x=1"));
        when(iconResolverRegistry.resolveIconUrl(any())).thenReturn(Optional.of("https://mangadex.org/icon.png"));

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.created()).hasSize(1);
        ComicReadingSource created = report.created().getFirst().source();
        assertThat(created.getSlug()).isEqualTo("mangadex");
        assertThat(created.getName()).isEqualTo("MangaDex");
        assertThat(created.getUrl()).isEqualTo("https://mangadex.org");
        assertThat(created.getIconUrl()).isEqualTo("https://mangadex.org/icon.png");
        assertThat(created.getStatus()).isEqualTo(ComicReadingSourceStatus.APPROVED);
        assertThat(created.getContributedBy()).isSameAs(admin);
        assertThat(created.getReviewedBy()).isSameAs(admin);
        assertThat(created.getReviewedAt()).isNotNull();
        verify(referenceRepository).save(savedReference.capture());
        assertThat(savedReference.getValue().getSource()).isSameAs(created);
        assertThat(savedReference.getValue().getProviderKey()).isEqualTo(PROVIDER);
        assertThat(savedReference.getValue().getExternalId()).isEqualTo("1");
    }

    @ParameterizedTest
    @EnumSource(ComicReadingSourceStatus.class)
    void sync_linksAnExistingSourceWithTheSameOriginWithoutChangingIt(ComicReadingSourceStatus status) {
        ComicReadingSource existing = source(1L, "mangadex", "Mangadex (manual)", "https://mangadex.org", status);
        when(sourceRepository.findAll()).thenReturn(List.of(existing));
        providerReports(PROVIDER, descriptor("1", "MangaDex", "https://mangadex.org/titles"));

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.linked()).containsExactly(new SyncedSource(PROVIDER, existing));
        assertThat(report.created()).isEmpty();
        assertThat(existing.getStatus()).isEqualTo(status);
        assertThat(existing.getName()).isEqualTo("Mangadex (manual)");
        verify(sourceRepository, never()).save(any());
        verify(referenceRepository).save(savedReference.capture());
        assertThat(savedReference.getValue().getSource()).isSameAs(existing);
        assertThat(savedReference.getValue().getExternalId()).isEqualTo("1");
    }

    @Test
    void sync_updatesTheOriginOfALinkedSourceWhenItsDomainChanges() {
        ComicReadingSource existing = source(1L, "olympus", "https://old.example");
        when(sourceRepository.findAll()).thenReturn(List.of(existing));
        when(referenceRepository.findByProviderKey(PROVIDER)).thenReturn(List.of(reference(existing, PROVIDER, "1")));
        providerReports(PROVIDER, descriptor("1", "Olympus", "https://new.example/home"));

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(existing.getUrl()).isEqualTo("https://new.example");
        assertThat(report.updated()).containsExactly(new SyncedSource(PROVIDER, existing));
        verify(sourceRepository).save(existing);
        verify(referenceRepository, never()).save(any());
    }

    @Test
    void sync_changesNothingForALinkedSourceWhoseOriginIsTheSame() {
        ComicReadingSource existing = source(1L, "olympus", "https://olympus.example");
        when(sourceRepository.findAll()).thenReturn(List.of(existing));
        when(referenceRepository.findByProviderKey(PROVIDER)).thenReturn(List.of(reference(existing, PROVIDER, "1")));
        providerReports(PROVIDER, descriptor("1", "Olympus", "https://olympus.example"));

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.created()).isEmpty();
        assertThat(report.linked()).isEmpty();
        assertThat(report.updated()).isEmpty();
        assertThat(report.skipped()).isEmpty();
        verify(sourceRepository, never()).save(any());
        verify(referenceRepository, never()).save(any());
    }

    @Test
    void sync_replacesTheExternalIdWhenTheSourceAlreadyHasAnotherOneFromTheProvider() {
        ComicReadingSource existing = source(1L, "mangadex", "https://mangadex.org");
        ComicReadingSourceProviderReference stale = reference(existing, PROVIDER, "old");
        when(sourceRepository.findAll()).thenReturn(List.of(existing));
        when(referenceRepository.findByProviderKey(PROVIDER)).thenReturn(List.of(stale));
        providerReports(PROVIDER, descriptor("new", "MangaDex", "https://mangadex.org"));

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.linked()).containsExactly(new SyncedSource(PROVIDER, existing));
        verify(referenceRepository).save(savedReference.capture());
        assertThat(savedReference.getValue()).isSameAs(stale);
        assertThat(stale.getExternalId()).isEqualTo("new");
    }

    @ParameterizedTest
    @CsvSource({ "'', NO_URL", "'   ', NO_URL", "not a url, INVALID_URL", "mangadex.org, INVALID_URL" })
    void sync_skipsADescriptorWhoseUrlHasNoUsableOrigin(String url, SkipReason reason) {
        ComicReadingSourceDescriptor descriptor = descriptor("1", "MangaDex", url);
        providerReports(PROVIDER, descriptor);

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.skipped()).containsExactly(new SkippedSource(PROVIDER, descriptor, reason));
        verify(sourceRepository, never()).save(any());
        verify(referenceRepository, never()).save(any());
    }

    @Test
    void sync_skipsASecondDescriptorOfTheSameProviderWithTheSameOrigin() {
        // Propósito: una extensión con un source por idioma publica varios descriptores con el mismo
        // origen, y solo cabe uno: solo hay una referencia por fuente y provider.
        // Flujo:
        // 1. El provider informa de dos descriptores con ids distintos y el mismo origen.
        // 2. Se crea la fuente con el primero.
        // 3. El segundo se salta como DUPLICATE_URL y no crea otra fuente ni otra referencia.
        ComicReadingSourceDescriptor english = descriptor("en", "Site (EN)", "https://site.example");
        ComicReadingSourceDescriptor spanish = descriptor("es", "Site (ES)", "https://site.example");
        providerReports(PROVIDER, english, spanish);

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.created()).hasSize(1);
        assertThat(report.created().getFirst().source().getName()).isEqualTo("Site (EN)");
        assertThat(report.skipped()).containsExactly(new SkippedSource(PROVIDER, spanish, SkipReason.DUPLICATE_URL));
        verify(referenceRepository).save(any());
    }

    @Test
    void sync_linksTheSourceCreatedByAnotherProviderInTheSameRun() {
        // Propósito: dos providers que informan del mismo sitio acaban con una sola fuente y una
        // referencia cada uno, aunque la fuente aún no exista en la BD al empezar.
        // Flujo:
        // 1. Dos providers informan del mismo origen.
        // 2. El primero crea la fuente.
        // 3. El segundo no la crea otra vez: la vincula.
        when(providerRegistry.discoverAll()).thenReturn(List.of(
                new DiscoveryOutcome.Discovered("first", List.of(descriptor("a", "Site", "https://site.example"))),
                new DiscoveryOutcome.Discovered("second", List.of(descriptor("z", "Site", "https://site.example")))));

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.created()).hasSize(1);
        assertThat(report.linked()).hasSize(1);
        assertThat(report.linked().getFirst().providerKey()).isEqualTo("second");
        assertThat(report.linked().getFirst().source()).isSameAs(report.created().getFirst().source());
    }

    // Comportamiento problemático, tomamos la decisión prudente de no fusionar 
    @Test
    void sync_skipsAnOriginChangeThatBelongsToAnotherSource() {
        ComicReadingSource linked = source(1L, "old", "https://old.example");
        ComicReadingSource other = source(2L, "other", "https://taken.example");
        ComicReadingSourceDescriptor descriptor = descriptor("1", "Old", "https://taken.example");
        when(sourceRepository.findAll()).thenReturn(List.of(linked, other));
        when(referenceRepository.findByProviderKey(PROVIDER)).thenReturn(List.of(reference(linked, PROVIDER, "1")));
        providerReports(PROVIDER, descriptor);

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.skipped()).containsExactly(new SkippedSource(PROVIDER, descriptor, SkipReason.URL_IN_USE));
        assertThat(linked.getUrl()).isEqualTo("https://old.example");
        verify(sourceRepository, never()).save(any());
    }

    @Test
    void sync_addsANumericSuffixToTheSlugWhenItIsAlreadyTaken() {
        // Propósito: varias fuentes con el mismo nombre (p. ej. una por idioma) no chocan en el slug,
        // ni con las que ya existían ni entre las que crea la propia ejecución.
        // Flujo:
        // 1. Ya existe una fuente con el slug "mangadex".
        // 2. El provider informa de dos fuentes más llamadas "MangaDex", con orígenes distintos.
        // 3. Reciben "mangadex-2" y "mangadex-3".
        when(sourceRepository.findAll()).thenReturn(List.of(source(1L, "mangadex", "https://mangadex.org")));
        providerReports(PROVIDER, descriptor("1", "MangaDex", "https://es.mangadex.example"),
                descriptor("2", "MangaDex", "https://fr.mangadex.example"));

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.created()).extracting(created -> created.source().getSlug())
                .containsExactly("mangadex-2", "mangadex-3");
    }

    @Test
    void sync_reportsAProviderThatFailedAndStillSyncsTheOthers() {
        DiscoveryOutcome.Failed failed = new DiscoveryOutcome.Failed("down", new IllegalStateException("down"));
        when(providerRegistry.discoverAll()).thenReturn(List.of(failed,
                new DiscoveryOutcome.Discovered(PROVIDER, List.of(descriptor("1", "Site", "https://site.example")))));

        ComicReadingSourceSyncReport report = service.sync(ADMIN_ID);

        assertThat(report.failedProviders()).containsExactly(failed);
        assertThat(report.created()).hasSize(1);
    }

}
