package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.Slugifier;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
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
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotApprovedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotFoundException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingEntryException;
import com.github.kio7po.comic_tracker.domain.exceptions.DuplicateComicReadingSourceException;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryProviderReferenceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryTarget;

@ExtendWith(MockitoExtension.class)
class ComicReadingEntryServiceTest {

    private static final Long COMIC_ID = 1L;
    private static final Long SOURCE_ID = 2L;
    private static final Long ENTRY_ID = 3L;
    private static final Long USER_ID = 4L;
    private static final String URL = "https://example.com/berserk";
    private static final String LOCALE = "es-ES";
    private static final String PROVIDER_KEY = "olympus";
    private static final Duration TTL = Duration.ofHours(6);
    private static final Duration RETRY_BACKOFF = Duration.ofMinutes(30);

    @Mock
    private ComicReadingEntryRepository comicReadingEntryRepository;
    @Mock
    private ComicReadingEntryProviderReferenceRepository entryProviderReferenceRepository;
    @Mock
    private ComicReadingSourceRepository comicReadingSourceRepository;
    @Mock
    private ComicRepository comicRepository;
    @Mock
    private ComicReadingSourceIconResolverRegistry iconResolverRegistry;
    @Mock
    private ComicReadingProviderRegistry readingProviderRegistry;
    @Mock
    private UserService userService;
    // Un captor atrapa el argumento con el que el servicio llamó a un mock, para poder inspeccionarlo
    // después. Hace falta porque el servicio crea la referencia por dentro: el test no la tiene a mano
    // para pasársela a verify(), y compararla con equals() no serviría (la entidad no lo redefine).
    // Uso: verify(mock).metodo(captor.capture()) atrapa el argumento; captor.getValue() lo devuelve.
    @Captor
    private ArgumentCaptor<ComicReadingEntryProviderReference> referenceCaptor;
    // Igual que el anterior, para la lista de referencias que se pasa a saveAll.
    @Captor
    private ArgumentCaptor<List<ComicReadingEntryProviderReference>> referencesCaptor;

    private ComicReadingEntryService comicReadingEntryService;

    @BeforeEach
    void setUp() {
        comicReadingEntryService = new ComicReadingEntryService(comicReadingEntryRepository,
                entryProviderReferenceRepository, comicReadingSourceRepository, comicRepository,
                iconResolverRegistry, readingProviderRegistry, userService, TTL, RETRY_BACKOFF);
        // Por defecto ningún provider sirve la fuente; los tests que necesitan otra respuesta la declaran.
        lenient().when(readingProviderRegistry.fetch(any(ComicReadingSource.class), any(ComicReadingEntryTarget.class)))
                .thenReturn(new FetchOutcome.NotSupported());
    }

    private static ComicReadingEntryDetails details(String externalId, String url, String title,
            Instant latestChapterAt) {
        return new ComicReadingEntryDetails(externalId, url, title, 374, latestChapterAt);
    }

    private static FetchOutcome found(ComicReadingEntryDetails details) {
        return new FetchOutcome.Found(new FetchedEntry(PROVIDER_KEY, details));
    }

    @Test
    void submitThrowsWhenComicDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(ComicNotFoundException.class);

        verify(comicReadingSourceRepository, never()).findById(any());
    }

    @Test
    void submitThrowsWhenSourceDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(ComicReadingSourceNotFoundException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitThrowsWhenEntryAlreadyExists() {
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();

        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, URL))
                .thenReturn(Optional.of(new ComicReadingEntry()));

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(DuplicateComicReadingEntryException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitPersistsPendingEntryWithContributor() {
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        User contributor = new User();

        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, URL)).thenReturn(Optional.empty());
        when(userService.findById(USER_ID)).thenReturn(contributor);
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID);

        assertThat(result.getComic()).isSameAs(comic);
        assertThat(result.getSource()).isSameAs(source);
        assertThat(result.getUrl()).isEqualTo(URL);
        assertThat(result.getLocale()).isEqualTo(LOCALE);
        assertThat(result.getContributedBy()).isSameAs(contributor);
        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.PENDING);
    }

    @Test
    void submitStoresTheCanonicalUrlAndHydratesTheEntryFromTheProvider() {
        // Propósito: al enviar una entry de un sitio con provider, la entry se guarda ya hidratada y con
        // la URL canónica del provider en lugar de la que pegó el usuario, y se guarda también la
        // referencia con el identificador que ese provider le dio a la obra.
        //
        // Flujo: el usuario envía una URL; el registro (simulado) responde con otra URL canónica y los
        // datos de la obra. La entry guardada tiene la URL canónica, el título, los capítulos, la fecha
        // del último y lastFetchedAt rellenado (para no refrescarla enseguida). Su referencia lleva la
        // clave del provider que respondió y el externalId "42".
        String canonicalUrl = "https://example.com/berserk-canonical";
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        ComicReadingEntryDetails details = details("42", canonicalUrl, "Berserk",
                Instant.parse("2024-06-01T10:00:00Z"));
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.fetch(source, new ComicReadingEntryTarget(URL, null))).thenReturn(found(details));
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID);

        assertThat(result.getUrl()).isEqualTo(canonicalUrl);
        assertThat(result.getTitle()).isEqualTo("Berserk");
        assertThat(result.getAvailableChapters()).isEqualTo(374);
        assertThat(result.getLatestChapterAt()).isEqualTo(Instant.parse("2024-06-01T10:00:00Z"));
        assertThat(result.getLastFetchedAt()).isNotNull();
        // capture() atrapa la referencia que el servicio le pasó a save(); getValue() la devuelve.
        verify(entryProviderReferenceRepository).save(referenceCaptor.capture());
        assertThat(referenceCaptor.getValue().getEntry()).isSameAs(result);
        assertThat(referenceCaptor.getValue().getProviderKey()).isEqualTo(PROVIDER_KEY);
        assertThat(referenceCaptor.getValue().getExternalId()).isEqualTo("42");
    }

    @Test
    void submitDoesNotSaveAReferenceWhenTheProviderGivesNoExternalId() {
        // Propósito: no todos los sitios dan un identificador estable de la obra; sin él la entry se
        // hidrata igualmente, pero no hay referencia que guardar ni identificador con el que buscar
        // duplicados.
        //
        // Flujo: el registro responde Found con externalId = null. La entry queda hidratada, no se guarda
        // ninguna referencia y tampoco se consulta si otra entry tiene ya ese identificador.
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.fetch(source, new ComicReadingEntryTarget(URL, null)))
                .thenReturn(found(details(null, URL, "Berserk", null)));
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID);

        assertThat(result.getTitle()).isEqualTo("Berserk");
        verify(entryProviderReferenceRepository, never()).save(any());
        verify(entryProviderReferenceRepository, never())
                .existsByEntryComicAndEntrySourceAndProviderKeyAndExternalId(any(), any(), any(), any());
    }

    @Test
    void submitThrowsWhenAnotherEntryAlreadyHasTheSameExternalIdInTheSameProvider() {
        // Propósito: un envío se rechaza como duplicado aunque su URL sea distinta, porque dos URLs
        // pueden apuntar a la misma obra y eso solo lo revela el provider, a través del externalId.
        //
        // Flujo: la URL canónica ("berserk-new") no existe todavía, pero el repositorio de referencias
        // indica que ya hay una entry de ese cómic y fuente con externalId "42" en ese mismo provider. Se
        // lanza la excepción de duplicado y no se guarda nada.
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        ComicReadingEntryDetails details = details("42", "https://example.com/berserk-new", "Berserk", null);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.fetch(source, new ComicReadingEntryTarget(URL, null))).thenReturn(found(details));
        when(entryProviderReferenceRepository.existsByEntryComicAndEntrySourceAndProviderKeyAndExternalId(comic,
                source, PROVIDER_KEY, "42")).thenReturn(true);

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(DuplicateComicReadingEntryException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitThrowsWhenTheProviderSupportsTheSourceButFindsNoComicAtTheUrl() {
        // Propósito: es el único caso en que un provider rechaza un envío: el provider funciona,
        // sirve el sitio y responde que en esa URL no hay ninguna obra.
        //
        // Flujo: el registro responde NotFound (el provider respondió y no encontró nada). Se lanza
        // ComicNotFoundInReadingSourceException con la URL y no se guarda nada.
        ComicReadingSource source = new ComicReadingSource();
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.fetch(source, new ComicReadingEntryTarget(URL, null)))
                .thenReturn(new FetchOutcome.NotFound());

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(ComicNotFoundInReadingSourceException.class)
                .extracting(exception -> ((ComicNotFoundInReadingSourceException) exception).getUrl())
                .isEqualTo(URL);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitSavesTheEntryUnhydratedWhenNoProviderSupportsTheSource() {
        // Propósito: que ningún provider sirva la fuente no significa "la obra no existe": la entry se
        // guarda tal cual, sin hidratar.
        //
        // Flujo: el registro responde NotSupported (es también lo que responde por defecto en estos
        // tests). No se rechaza: la entry se guarda con la URL original y sin referencia.
        ComicReadingSource source = new ComicReadingSource();
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID);

        assertThat(result.getUrl()).isEqualTo(URL);
        assertThat(result.getLastFetchedAt()).isNull();
        verify(entryProviderReferenceRepository, never()).save(any());
    }

    @Test
    void submitSavesTheEntryUnhydratedWhenTheProviderFails() {
        // Propósito: una caída o un error del provider no es una respuesta sobre la obra: no bloquea la
        // aportación ni se confunde con "la obra no existe".
        //
        // Flujo: el registro responde Failed y la entry se guarda sin hidratar y sin referencia.
        ComicReadingSource source = new ComicReadingSource();
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.fetch(source, new ComicReadingEntryTarget(URL, null)))
                .thenReturn(new FetchOutcome.Failed(new RuntimeException("site is down")));
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID);

        assertThat(result.getUrl()).isEqualTo(URL);
        assertThat(result.getTitle()).isNull();
        assertThat(result.getLastFetchedAt()).isNull();
        verify(entryProviderReferenceRepository, never()).save(any());
    }

    @Test
    void submitWithNewSourceThrowsWhenComicDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.submitWithNewSource(COMIC_ID, "MangaDex",
                "https://mangadex.org", URL, LOCALE, USER_ID)).isInstanceOf(ComicNotFoundException.class);
    }

    @Test
    void submitWithNewSourceThrowsWhenSourceUrlAlreadyRegistered() {
        String sourceUrl = "https://mangadex.org/title/123/chapter-5";
        ComicReadingSource existing = new ComicReadingSource();
        existing.setId(SOURCE_ID);

        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findByUrl(UrlNormalizer.normalizeOrigin(sourceUrl)))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> comicReadingEntryService.submitWithNewSource(COMIC_ID, "MangaDex", sourceUrl, URL,
                LOCALE, USER_ID)).isInstanceOf(DuplicateComicReadingSourceException.class);

        verify(comicReadingSourceRepository, never()).save(any());
        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitWithNewSourceCreatesSourceWithURLCollapsedToOriginAndEntry() {
        String sourceUrl = "https://mangadex.org/title/123/chapter-5";
        String iconUrl = "https://mangadex.org/icon.png";
        Comic comic = new Comic();
        User contributor = new User();

        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(userService.findById(USER_ID)).thenReturn(contributor);
        when(comicReadingSourceRepository.findByUrl(UrlNormalizer.normalizeOrigin(sourceUrl)))
                .thenReturn(Optional.empty());
        when(comicReadingSourceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(iconResolverRegistry.resolveIconUrl(any())).thenReturn(Optional.of(iconUrl));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(eq(comic), any(), eq(URL)))
                .thenReturn(Optional.empty());
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submitWithNewSource(COMIC_ID, "MangaDex", sourceUrl, URL,
                LOCALE, USER_ID);

        ComicReadingSource source = result.getSource();
        assertThat(source.getUrl()).isEqualTo(UrlNormalizer.normalizeOrigin(sourceUrl));
        assertThat(source.getSlug()).isEqualTo(Slugifier.slugify("MangaDex"));
        assertThat(source.getName()).isEqualTo("MangaDex");
        assertThat(source.getIconUrl()).isEqualTo(iconUrl);
        assertThat(source.getContributedBy()).isSameAs(contributor);
        assertThat(source.getStatus()).isEqualTo(ComicReadingSourceStatus.PENDING);
        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.PENDING);
    }

    @Test
    void findByComicThrowsWhenComicDoesNotExist() {
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.findByComic(COMIC_ID, null))
                .isInstanceOf(ComicNotFoundException.class);
    }

    @Test
    void findByComicReturnsEveryEntryWhenNoStatusFilterGiven() {
        Comic comic = new Comic();
        List<ComicReadingEntry> entries = List.of(new ComicReadingEntry(), new ComicReadingEntry());
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(entries);
        when(readingProviderRegistry.fetchAll(entries))
                .thenReturn(List.of(new FetchOutcome.NotSupported(), new FetchOutcome.NotSupported()));

        assertThat(comicReadingEntryService.findByComic(COMIC_ID, null)).isSameAs(entries);

        verify(comicReadingEntryRepository, never()).findByComicAndStatus(any(), any());
    }

    @Test
    void findByComicReturnsOnlyEntriesMatchingTheGivenStatus() {
        Comic comic = new Comic();
        List<ComicReadingEntry> pendingEntries = List.of(new ComicReadingEntry());
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComicAndStatus(comic, ComicReadingEntryStatus.PENDING))
                .thenReturn(pendingEntries);
        when(readingProviderRegistry.fetchAll(pendingEntries)).thenReturn(List.of(new FetchOutcome.NotSupported()));

        assertThat(comicReadingEntryService.findByComic(COMIC_ID, ComicReadingEntryStatus.PENDING))
                .isSameAs(pendingEntries);

        verify(comicReadingEntryRepository, never()).findByComic(any());
    }

    @Test
    void findByComicHydratesAStaleEntryViaTheRegistryAndSavesItWithItsReference() {
        // Propósito: una entry caducada se hidrata con lo que devuelve el registro y se guarda, junto con
        // la referencia al identificador que su provider le da a la obra.
        //
        // Flujo: una entry aprobada sin lastFetchedAt (caducada). El registro responde Found con los datos
        // de la obra. La entry recibe título, capítulos, fecha y lastFetchedAt; se guarda con saveAll y,
        // como no tenía referencia con ese provider, se crea una nueva con el externalId "42".
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetchAll(List.of(entry)))
                .thenReturn(List.of(found(details("42", URL, "Berserk", Instant.parse("2024-06-01T10:00:00Z")))));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getTitle()).isEqualTo("Berserk");
        assertThat(entry.getAvailableChapters()).isEqualTo(374);
        assertThat(entry.getLatestChapterAt()).isEqualTo(Instant.parse("2024-06-01T10:00:00Z"));
        assertThat(entry.getLastFetchedAt()).isNotNull();
        verify(comicReadingEntryRepository).saveAll(List.of(entry));
        // capture() atrapa la lista de referencias que el servicio le pasó a saveAll(); getValue() la devuelve.
        verify(entryProviderReferenceRepository).saveAll(referencesCaptor.capture());
        assertThat(referencesCaptor.getValue()).hasSize(1);
        ComicReadingEntryProviderReference reference = referencesCaptor.getValue().getFirst();
        assertThat(reference.getEntry()).isSameAs(entry);
        assertThat(reference.getProviderKey()).isEqualTo(PROVIDER_KEY);
        assertThat(reference.getExternalId()).isEqualTo("42");
    }

    @Test
    void findByComicUpdatesTheExistingReferenceInsteadOfCreatingAnother() {
        // Propósito: si la entry ya tenía una referencia con el provider que respondió, se actualiza su
        // identificador en lugar de crear una segunda fila (que rompería la unicidad entry + provider).
        //
        // Flujo: la entry tiene ya una referencia con ese provider y externalId "41". El registro responde
        // con externalId "42". Lo que se guarda es esa misma referencia, ya con "42".
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setId(ENTRY_ID);
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        ComicReadingEntryProviderReference existing = new ComicReadingEntryProviderReference();
        existing.setEntry(entry);
        existing.setProviderKey(PROVIDER_KEY);
        existing.setExternalId("41");
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetchAll(List.of(entry)))
                .thenReturn(List.of(found(details("42", URL, "Berserk", null))));
        when(entryProviderReferenceRepository.findByEntryIn(List.of(entry))).thenReturn(List.of(existing));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        verify(entryProviderReferenceRepository).saveAll(referencesCaptor.capture());
        assertThat(referencesCaptor.getValue()).containsExactly(existing);
        assertThat(existing.getExternalId()).isEqualTo("42");
    }

    @Test
    void findByComicDoesNotSaveAReferenceWhenTheProviderGivesNoExternalId() {
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetchAll(List.of(entry)))
                .thenReturn(List.of(found(details(null, URL, "Berserk", null))));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getTitle()).isEqualTo("Berserk");
        verify(entryProviderReferenceRepository, never()).saveAll(any());
    }

    private static Stream<Arguments> outcomesWithoutNewData() {
        return Stream.of(
                Arguments.of("work not found", new FetchOutcome.NotFound()),
                Arguments.of("no provider serves the source", new FetchOutcome.NotSupported()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("outcomesWithoutNewData")
    void findByComicKeepsTheStoredDataAndWaitsForTheNextTtlWhenThereIsNothingNew(String caseName,
            FetchOutcome outcome) {
        // Propósito: cuando el refresco no trae datos nuevos pero tampoco ha fallado (la obra ya no
        // existe, o ningún provider sirve la fuente), la entry conserva lo que tenía y no se vuelve a
        // consultar hasta el siguiente TTL.
        //
        // Flujo: una entry caducada con título "Old". El registro responde sin datos. El título no cambia,
        // lastFetchedAt pasa a ser "ahora" (un TTL completo, no el reintento corto del fallo) y no se
        // escribe ninguna referencia.
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setTitle("Old");
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetchAll(List.of(entry))).thenReturn(List.of(outcome));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getTitle()).isEqualTo("Old");
        assertThat(entry.getLastFetchedAt()).isAfterOrEqualTo(before);
        verify(entryProviderReferenceRepository, never()).saveAll(any());
    }

    @Test
    void findByComicHydratesAPendingEntryToo() {
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.PENDING);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetchAll(List.of(entry)))
                .thenReturn(List.of(found(details("42", URL, "Berserk", null))));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getTitle()).isEqualTo("Berserk");
    }

    @Test
    void findByComicNeverRefreshesARejectedEntry() {
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.REJECTED);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        verify(readingProviderRegistry, never()).fetchAll(any());
        verify(comicReadingEntryRepository, never()).saveAll(any());
    }

    @Test
    void findByComicDoesNotRefreshAnEntryStillWithinTtl() {
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        entry.setLastFetchedAt(Instant.now());
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        verify(readingProviderRegistry, never()).fetchAll(any());
        verify(comicReadingEntryRepository, never()).saveAll(any());
    }

    @Test
    void findByComicAppliesBackoffInsteadOfAFullTtlWhenTheRegistryReturnsAFailure() {
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetchAll(List.of(entry)))
                .thenReturn(List.of(new FetchOutcome.Failed(new RuntimeException("site is down"))));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getTitle()).isNull();
        assertThat(entry.getLastFetchedAt()).isAfter(Instant.now().minus(TTL));
        assertThat(entry.getLastFetchedAt()).isBefore(Instant.now());
    }

    @Test
    void findByComicRefreshesEachStaleEntryIndependentlyWhenOneProviderCallFails() {
        Comic comic = new Comic();
        ComicReadingEntry failingEntry = new ComicReadingEntry();
        failingEntry.setUrl("https://example.com/failing");
        failingEntry.setStatus(ComicReadingEntryStatus.APPROVED);
        ComicReadingEntry succeedingEntry = new ComicReadingEntry();
        succeedingEntry.setUrl("https://example.com/succeeding");
        succeedingEntry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(failingEntry, succeedingEntry));
        when(readingProviderRegistry.fetchAll(List.of(failingEntry, succeedingEntry))).thenReturn(List.of(
                new FetchOutcome.Failed(new RuntimeException("site is down")),
                found(details("7", "https://example.com/succeeding", "One Piece", null))));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(failingEntry.getTitle()).isNull();
        assertThat(succeedingEntry.getTitle()).isEqualTo("One Piece");
    }

    @Test
    void findByComicMovesTheEntryToTheUrlTheProviderReturns() {
        // Propósito: cuando el sitio cambia la ruta de una obra, la entry pasa sola a la URL nueva al
        // refrescarse; la referencia al identificador de la obra es lo que permite seguir encontrándola.
        //
        // Flujo: una entry caducada con URL antigua. El provider devuelve la misma obra con una URL
        // nueva y el repositorio indica que nadie más tiene esa URL: la entry adopta la URL nueva.
        String movedUrl = "https://example.com/berserk-renamed";
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        ComicReadingEntry entry = staleEntry(comic, source, URL);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetchAll(List.of(entry)))
                .thenReturn(List.of(found(details("42", movedUrl, "Berserk", null))));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, movedUrl))
                .thenReturn(Optional.empty());
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getUrl()).isEqualTo(movedUrl);
    }

    @Test
    void findByComicKeepsTheOldUrlWhenAnotherEntryAlreadyHoldsTheNewOne() {
        // Propósito: la restricción de unicidad (cómic, fuente, URL) impide que dos entries tengan la
        // misma URL. Si otra entry ya tiene la URL nueva, mover esta haría fallar el guardado de todo
        // el lote, así que conserva la antigua.
        //
        // Flujo: una entry caducada con URL antigua; el provider devuelve una URL nueva, pero el
        // repositorio indica que YA existe otra entry con esa URL. La entry conserva la URL antigua
        // (sigue funcionando por su referencia). El resto sí se actualiza (título): solo se omite el
        // cambio de URL.
        String movedUrl = "https://example.com/berserk-renamed";
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        ComicReadingEntry entry = staleEntry(comic, source, URL);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetchAll(List.of(entry)))
                .thenReturn(List.of(found(details("42", movedUrl, "Berserk", null))));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, movedUrl))
                .thenReturn(Optional.of(new ComicReadingEntry()));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getUrl()).isEqualTo(URL);
        assertThat(entry.getTitle()).isEqualTo("Berserk");
    }

    @Test
    void findByComicDoesNotMoveTwoEntriesOfTheSameBatchToTheSameUrl() {
        // Propósito: dos entries que se refrescan en la misma lectura no pueden acabar con la misma URL,
        // aunque el repositorio no pueda detectarlo porque ninguna de las dos se ha guardado aún.
        //
        // Flujo: dos entries caducadas (URLs antiguas distintas) del mismo cómic y fuente; el provider
        // devuelve la misma URL nueva para las dos. Se procesan en orden: la primera se la queda y la
        // segunda ve que ya está reclamada y conserva la suya.
        String firstUrl = "https://example.com/berserk-old-1";
        String secondUrl = "https://example.com/berserk-old-2";
        String movedUrl = "https://example.com/berserk-renamed";
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        ComicReadingEntry first = staleEntry(comic, source, firstUrl);
        ComicReadingEntry second = staleEntry(comic, source, secondUrl);
        FetchOutcome outcome = found(details("42", movedUrl, "Berserk", null));
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(first, second));
        when(readingProviderRegistry.fetchAll(List.of(first, second))).thenReturn(List.of(outcome, outcome));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, movedUrl))
                .thenReturn(Optional.empty());
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(first.getUrl()).isEqualTo(movedUrl);
        assertThat(second.getUrl()).isEqualTo(secondUrl);
    }

    private static ComicReadingEntry staleEntry(Comic comic, ComicReadingSource source, String url) {
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setComic(comic);
        entry.setSource(source);
        entry.setUrl(url);
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        return entry;
    }

    @Test
    void approveThrowsWhenEntryDoesNotExist() {
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.approve(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingEntryNotFoundException.class);
    }

    @Test
    void approveThrowsWhenEntryWasAlreadyReviewed() {
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> comicReadingEntryService.approve(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingEntryAlreadyReviewedException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void approveThrowsWhenSourceIsNotApproved() {
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        source.setStatus(ComicReadingSourceStatus.PENDING);
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setSource(source);
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> comicReadingEntryService.approve(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingSourceNotApprovedException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void approveSetsStatusAndReviewer() {
        ComicReadingSource source = new ComicReadingSource();
        source.setStatus(ComicReadingSourceStatus.APPROVED);
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setSource(source);
        User reviewer = new User();
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));
        when(userService.findById(USER_ID)).thenReturn(reviewer);
        when(comicReadingEntryRepository.save(entry)).thenReturn(entry);

        ComicReadingEntry result = comicReadingEntryService.approve(ENTRY_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.APPROVED);
        assertThat(result.getReviewedBy()).isSameAs(reviewer);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void rejectThrowsWhenEntryDoesNotExist() {
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> comicReadingEntryService.reject(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingEntryNotFoundException.class);
    }

    @Test
    void rejectThrowsWhenEntryWasAlreadyReviewed() {
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> comicReadingEntryService.reject(ENTRY_ID, USER_ID))
                .isInstanceOf(ComicReadingEntryAlreadyReviewedException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void rejectDoesNotThrowWhenSourceIsNotApproved() {
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        source.setStatus(ComicReadingSourceStatus.PENDING);
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setSource(source);
        User reviewer = new User();
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));
        when(userService.findById(USER_ID)).thenReturn(reviewer);
        when(comicReadingEntryRepository.save(entry)).thenReturn(entry);

        ComicReadingEntry result = comicReadingEntryService.reject(ENTRY_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.REJECTED);
    }

    @Test
    void rejectSetsStatusAndReviewer() {
        ComicReadingEntry entry = new ComicReadingEntry();
        User reviewer = new User();
        when(comicReadingEntryRepository.findById(ENTRY_ID)).thenReturn(Optional.of(entry));
        when(userService.findById(USER_ID)).thenReturn(reviewer);
        when(comicReadingEntryRepository.save(entry)).thenReturn(entry);

        ComicReadingEntry result = comicReadingEntryService.reject(ENTRY_ID, USER_ID);

        assertThat(result.getStatus()).isEqualTo(ComicReadingEntryStatus.REJECTED);
        assertThat(result.getReviewedBy()).isSameAs(reviewer);
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void findByStatusInDelegatesToRepository() {
        List<ComicReadingEntryStatus> statuses = List.of(ComicReadingEntryStatus.PENDING);
        Page<ComicReadingEntry> page = new Page<>(List.of(new ComicReadingEntry()), false, 1);
        when(comicReadingEntryRepository.findByStatusIn(statuses, ComicReadingEntrySortField.CREATED_AT,
                SortDirection.ASC, 20, 0)).thenReturn(page);

        Page<ComicReadingEntry> result = comicReadingEntryService.findByStatusIn(statuses,
                ComicReadingEntrySortField.CREATED_AT, SortDirection.ASC, 20, 0);

        assertThat(result).isSameAs(page);
        verify(comicReadingEntryRepository).findByStatusIn(statuses, ComicReadingEntrySortField.CREATED_AT,
                SortDirection.ASC, 20, 0);
    }

}
