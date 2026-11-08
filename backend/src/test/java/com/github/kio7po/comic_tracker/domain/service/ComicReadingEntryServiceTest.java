package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.kio7po.comic_tracker.domain.common.Page;
import com.github.kio7po.comic_tracker.domain.common.Slugifier;
import com.github.kio7po.comic_tracker.domain.common.SortDirection;
import com.github.kio7po.comic_tracker.domain.common.UrlNormalizer;
import com.github.kio7po.comic_tracker.domain.entities.Comic;
import com.github.kio7po.comic_tracker.domain.entities.ComicReadingEntry;
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
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingEntryRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicReadingSourceRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.ComicRepository;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDetails;

@ExtendWith(MockitoExtension.class)
class ComicReadingEntryServiceTest {

    private static final Long COMIC_ID = 1L;
    private static final Long SOURCE_ID = 2L;
    private static final Long ENTRY_ID = 3L;
    private static final Long USER_ID = 4L;
    private static final String URL = "https://example.com/berserk";
    private static final String LOCALE = "es-ES";
    private static final Duration TTL = Duration.ofHours(6);
    private static final Duration RETRY_BACKOFF = Duration.ofMinutes(30);

    @Mock
    private ComicReadingEntryRepository comicReadingEntryRepository;
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

    private ComicReadingEntryService comicReadingEntryService;

    @BeforeEach
    void setUp() {
        comicReadingEntryService = new ComicReadingEntryService(comicReadingEntryRepository,
                comicReadingSourceRepository, comicRepository, iconResolverRegistry, readingProviderRegistry,
                userService, TTL, RETRY_BACKOFF);
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
        // la URL canónica del provider en lugar de la que pegó el usuario.
        //
        // Flujo: el usuario envía una URL; el registro (simulado) responde con otra URL canónica y los
        // datos de la obra. La entry guardada tiene la URL canónica, el externalId, el título, los
        // capítulos, la fecha del último y lastFetchedAt rellenado (para no refrescarla enseguida).
        String canonicalUrl = "https://example.com/berserk-canonical";
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("42", canonicalUrl, "Berserk", 374,
                Instant.parse("2024-06-01T10:00:00Z"));
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.fetch(URL, null)).thenReturn(Optional.of(details));
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID);

        assertThat(result.getUrl()).isEqualTo(canonicalUrl);
        assertThat(result.getExternalId()).isEqualTo("42");
        assertThat(result.getTitle()).isEqualTo("Berserk");
        assertThat(result.getAvailableChapters()).isEqualTo(374);
        assertThat(result.getLatestChapterAt()).isEqualTo(Instant.parse("2024-06-01T10:00:00Z"));
        assertThat(result.getLastFetchedAt()).isNotNull();
    }

    @Test
    void submitThrowsWhenAnotherEntryAlreadyHasTheSameExternalId() {
        // Propósito: un envío se rechaza como duplicado aunque su URL sea distinta, porque dos URLs
        // pueden apuntar a la misma obra y eso solo lo revela el provider, a través del externalId.
        //
        // Flujo: la URL canónica ("berserk-new") no existe todavía, pero el repositorio indica que ya
        // hay una entry de ese cómic y fuente con externalId "42". Se lanza la excepción de duplicado
        // y no se guarda nada.
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("42", "https://example.com/berserk-new",
                "Berserk", 374, null);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(source));
        when(readingProviderRegistry.fetch(URL, null)).thenReturn(Optional.of(details));
        when(comicReadingEntryRepository.existsByComicAndSourceAndExternalId(comic, source, "42")).thenReturn(true);

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(DuplicateComicReadingEntryException.class);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitThrowsWhenTheProviderSupportsTheSiteButFindsNoComicAtTheUrl() {
        // Propósito: es el único caso en que un provider rechaza un envío: el provider funciona,
        // soporta el sitio y responde que en esa URL no hay ninguna obra.
        //
        // Flujo: fetch devuelve vacío (el provider respondió y no encontró nada) y supports(URL) es
        // true (hay provider para el sitio). Esa combinación significa "la obra no existe": se lanza
        // ComicNotFoundInReadingSourceException con la URL y no se guarda nada.
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(new ComicReadingSource()));
        when(readingProviderRegistry.fetch(URL, null)).thenReturn(Optional.empty());
        when(readingProviderRegistry.supports(URL)).thenReturn(true);

        assertThatThrownBy(() -> comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID))
                .isInstanceOf(ComicNotFoundInReadingSourceException.class)
                .extracting(exception -> ((ComicNotFoundInReadingSourceException) exception).getUrl())
                .isEqualTo(URL);

        verify(comicReadingEntryRepository, never()).save(any());
    }

    @Test
    void submitSavesTheEntryUnhydratedWhenNoProviderSupportsTheSite() {
        // Propósito: un resultado vacío por sí solo no significa "la obra no existe": también ocurre
        // cuando no hay ningún provider para el sitio. En ese caso la entry se guarda tal cual, sin
        // hidratar.
        //
        // Flujo: fetch devuelve vacío y supports(URL) es false (ningún provider soporta el sitio). No
        // se rechaza: la entry se guarda con la URL original y sin externalId.
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(new ComicReadingSource()));
        when(readingProviderRegistry.fetch(URL, null)).thenReturn(Optional.empty());
        when(readingProviderRegistry.supports(URL)).thenReturn(false);
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID);

        assertThat(result.getUrl()).isEqualTo(URL);
        assertThat(result.getExternalId()).isNull();
        assertThat(result.getLastFetchedAt()).isNull();
    }

    @Test
    void submitSavesTheEntryUnhydratedWhenTheProviderFails() {
        // Propósito: una caída o un error del provider no es una respuesta sobre la obra: no bloquea la
        // aportación ni se confunde con "la obra no existe".
        //
        // Flujo: fetch lanza una excepción y la entry se guarda sin hidratar. Como el provider falló,
        // ni siquiera se consulta supports(): es lo que se verifica al final del test.
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(new Comic()));
        when(comicReadingSourceRepository.findById(SOURCE_ID)).thenReturn(Optional.of(new ComicReadingSource()));
        when(readingProviderRegistry.fetch(URL, null)).thenThrow(new RuntimeException("site is down"));
        when(comicReadingEntryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ComicReadingEntry result = comicReadingEntryService.submit(COMIC_ID, SOURCE_ID, URL, LOCALE, USER_ID);

        assertThat(result.getUrl()).isEqualTo(URL);
        assertThat(result.getExternalId()).isNull();
        assertThat(result.getTitle()).isNull();
        assertThat(result.getLastFetchedAt()).isNull();
        verify(readingProviderRegistry, never()).supports(any());
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

        assertThat(comicReadingEntryService.findByComic(COMIC_ID, ComicReadingEntryStatus.PENDING))
                .isSameAs(pendingEntries);

        verify(comicReadingEntryRepository, never()).findByComic(any());
    }

    @Test
    void findByComicHydratesAStaleEntryViaTheRegistryAndSavesIt() {
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("42", URL, "Berserk", 374,
                Instant.parse("2024-06-01T10:00:00Z"));
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetch(URL, null)).thenReturn(Optional.of(details));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getExternalId()).isEqualTo("42");
        assertThat(entry.getTitle()).isEqualTo("Berserk");
        assertThat(entry.getAvailableChapters()).isEqualTo(374);
        assertThat(entry.getLatestChapterAt()).isEqualTo(Instant.parse("2024-06-01T10:00:00Z"));
        assertThat(entry.getLastFetchedAt()).isNotNull();
        verify(comicReadingEntryRepository).saveAll(List.of(entry));
    }

    @Test
    void findByComicHydratesAPendingEntryToo() {
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.PENDING);
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("42", URL, "Berserk", 374, null);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetch(URL, null)).thenReturn(Optional.of(details));
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

        verify(readingProviderRegistry, never()).fetch(any(), any());
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

        verify(readingProviderRegistry, never()).fetch(any(), any());
        verify(comicReadingEntryRepository, never()).saveAll(any());
    }

    @Test
    void findByComicAppliesBackoffInsteadOfAFullTtlWhenTheRegistryFails() {
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetch(URL, null)).thenThrow(new RuntimeException("site is down"));
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
        when(readingProviderRegistry.fetch("https://example.com/failing", null))
                .thenThrow(new RuntimeException("site is down"));
        when(readingProviderRegistry.fetch("https://example.com/succeeding", null)).thenReturn(Optional
                .of(new ComicReadingSourceDetails("7", "https://example.com/succeeding", "One Piece", 1000, null)));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(failingEntry.getTitle()).isNull();
        assertThat(succeedingEntry.getTitle()).isEqualTo("One Piece");
    }

    @Test
    void findByComicPassesTheStoredExternalIdToTheRegistry() {
        Comic comic = new Comic();
        ComicReadingEntry entry = new ComicReadingEntry();
        entry.setUrl(URL);
        entry.setExternalId("42");
        entry.setStatus(ComicReadingEntryStatus.APPROVED);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetch(URL, "42"))
                .thenReturn(Optional.of(new ComicReadingSourceDetails("42", URL, "Berserk", 374, null)));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        verify(readingProviderRegistry).fetch(URL, "42");
    }

    @Test
    void findByComicMovesTheEntryToTheUrlTheProviderReturns() {
        // Propósito: cuando el sitio cambia la ruta de una obra, la entry pasa sola a la URL nueva al
        // refrescarse; para eso existe el externalId.
        //
        // Flujo: una entry caducada con URL antigua. El provider devuelve la misma obra con una URL
        // nueva y el repositorio indica que nadie más tiene esa URL: la entry adopta la URL nueva y
        // guarda el externalId.
        String movedUrl = "https://example.com/berserk-renamed";
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        ComicReadingEntry entry = staleEntry(comic, source, URL);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetch(URL, null))
                .thenReturn(Optional.of(new ComicReadingSourceDetails("42", movedUrl, "Berserk", 374, null)));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, movedUrl))
                .thenReturn(Optional.empty());
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getUrl()).isEqualTo(movedUrl);
        assertThat(entry.getExternalId()).isEqualTo("42");
    }

    @Test
    void findByComicKeepsTheOldUrlWhenAnotherEntryAlreadyHoldsTheNewOne() {
        // Propósito: la restricción de unicidad (cómic, fuente, URL) impide que dos entries tengan la
        // misma URL. Si otra entry ya tiene la URL nueva, mover esta haría fallar el guardado de todo
        // el lote, así que conserva la antigua.
        //
        // Flujo: una entry caducada con URL antigua; el provider devuelve una URL nueva, pero el
        // repositorio indica que YA existe otra entry con esa URL. La entry conserva la URL antigua
        // (sigue funcionando por el externalId). El resto sí se actualiza (externalId, título): solo
        // se omite el cambio de URL.
        String movedUrl = "https://example.com/berserk-renamed";
        Comic comic = new Comic();
        ComicReadingSource source = new ComicReadingSource();
        source.setId(SOURCE_ID);
        ComicReadingEntry entry = staleEntry(comic, source, URL);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(entry));
        when(readingProviderRegistry.fetch(URL, null))
                .thenReturn(Optional.of(new ComicReadingSourceDetails("42", movedUrl, "Berserk", 374, null)));
        when(comicReadingEntryRepository.findByComicAndSourceAndUrl(comic, source, movedUrl))
                .thenReturn(Optional.of(new ComicReadingEntry()));
        when(comicReadingEntryRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        comicReadingEntryService.findByComic(COMIC_ID, null);

        assertThat(entry.getUrl()).isEqualTo(URL);
        assertThat(entry.getExternalId()).isEqualTo("42");
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
        ComicReadingSourceDetails details = new ComicReadingSourceDetails("42", movedUrl, "Berserk", 374, null);
        when(comicRepository.findById(COMIC_ID)).thenReturn(Optional.of(comic));
        when(comicReadingEntryRepository.findByComic(comic)).thenReturn(List.of(first, second));
        when(readingProviderRegistry.fetch(firstUrl, null)).thenReturn(Optional.of(details));
        when(readingProviderRegistry.fetch(secondUrl, null)).thenReturn(Optional.of(details));
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
