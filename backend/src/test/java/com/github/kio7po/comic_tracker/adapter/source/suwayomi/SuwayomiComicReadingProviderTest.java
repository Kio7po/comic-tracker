package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingCoverImage;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryTarget;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceTarget;

class SuwayomiComicReadingProviderTest {

    private static final String GRAPHQL_URL = "http://localhost:4567/api/graphql";
    private static final String SITE_URL = "https://www.leercapitulo.co";
    private static final String SUWAYOMI_SOURCE_ID = "5621901596522308538";
    private static final String OTHER_SUWAYOMI_SOURCE_ID = "5943212552955476721";
    private static final String COMIC_URL = SITE_URL + "/manga/wj10bv5s3y/solo-leveling/";
    // Fuente ya vinculada a Suwayomi (con su id) y fuente sin vincular (solo la URL).
    private static final ComicReadingSourceTarget LINKED_SOURCE = new ComicReadingSourceTarget(SITE_URL,
            SUWAYOMI_SOURCE_ID);
    private static final ComicReadingSourceTarget UNLINKED_SOURCE = new ComicReadingSourceTarget(SITE_URL, null);
    private static final String SOURCES_JSON = """
            { "data": { "sources": { "nodes": [
              { "id": "0", "name": "Local source", "lang": "localsourcelang", "homeUrl": null },
              { "id": "5621901596522308538", "name": "LeerCapitulo", "lang": "es",
                "homeUrl": "https://www.leercapitulo.co" }
            ] } } }
            """;
    private static final String MANGA_AND_CHAPTERS_JSON = """
            { "data": { "fetchMangaAndChapters": {
              "manga": { "id": 2, "title": "Solo Leveling", "realUrl": "%s" },
              "chapters": [ { "uploadDate": "1700000000000" }, { "uploadDate": "1710000000000" } ]
            } } }
            """.formatted(COMIC_URL);

    private MockRestServiceServer server;
    private SuwayomiGraphQlClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new SuwayomiGraphQlClient(builder, "http://localhost:4567");
    }

    private SuwayomiComicReadingProvider provider() {
        return provider(Duration.ofMinutes(10), registryWith(RateLimiterConfig.ofDefaults()));
    }

    private SuwayomiComicReadingProvider provider(Duration suwayomiSourcesTtl, RateLimiterRegistry registry) {
        return new SuwayomiComicReadingProvider(client, suwayomiSourcesTtl, registry);
    }

    // The provider creates its limiters from a config named "suwayomi", which the default registry doesn't have.
    private static RateLimiterRegistry registryWith(RateLimiterConfig config) {
        return RateLimiterRegistry.custom()
                .addRateLimiterConfig(SuwayomiComicReadingProvider.RATE_LIMITER_CONFIG_NAME, config)
                .build();
    }

    private void expectSources() {
        server.expect(requestTo(GRAPHQL_URL)).andRespond(withSuccess(SOURCES_JSON, MediaType.APPLICATION_JSON));
    }

    private void expectMangaAndChapters() {
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(jsonPath("$.variables.id").value(2))
                .andRespond(withSuccess(MANGA_AND_CHAPTERS_JSON, MediaType.APPLICATION_JSON));
    }

    @Test
    void fetchCover_asksSuwayomiForTheThumbnailOfThatWork() {
        server.expect(requestTo("http://localhost:4567/api/v1/manga/2/thumbnail"))
                .andRespond(withSuccess(new byte[] { 1, 2 }, MediaType.IMAGE_PNG));

        Optional<ComicReadingCoverImage> cover = provider().fetchCover("2");

        assertThat(cover).hasValueSatisfying(image -> assertThat(image.getContentType()).isEqualTo("image/png"));
        server.verify();
    }

    @Test
    void fetchCover_rejectsACoverIdThatIsNotANumberWithoutCallingSuwayomi() {
        // Propósito: la ruta de la miniatura se construye con el id de la portada, así que un valor que no sea un
        // número nunca puede llegar a Suwayomi como parte de una ruta.
        //
        // Flujo: se pide una portada con un id que intenta salirse de la ruta. Se lanza IllegalArgumentException y
        // el servidor simulado, que no espera ninguna petición, no recibe ninguna.
        SuwayomiComicReadingProvider provider = provider();

        assertThatThrownBy(() -> provider.fetchCover("2/../../graphql"))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test
    void key_isSuwayomi() {
        assertThat(provider().key()).isEqualTo("suwayomi");
    }

    @Test
    void discoverSources_returnsTheSourcesWithAHomeUrlAndLeavesOutTheLocalSource() {
        expectSources();

        List<ComicReadingSourceDescriptor> descriptors = provider().discoverSources();

        assertThat(descriptors).containsExactly(
                new ComicReadingSourceDescriptor(SUWAYOMI_SOURCE_ID, "LeerCapitulo", SITE_URL));
    }

    @Test
    void supports_isTrueForTheHostOfASuwayomiSourceAndFalseForAnyOther() {
        expectSources();
        SuwayomiComicReadingProvider provider = provider();

        assertThat(provider.supports(COMIC_URL)).isTrue();
        assertThat(provider.supports("https://other.example/manga/1")).isFalse();
    }

    @Test
    void supports_throwsWhenSuwayomiCannotBeReached() {
        server.expect(requestTo(GRAPHQL_URL)).andRespond(withServerError());

        SuwayomiComicReadingProvider provider = provider();

        assertThatThrownBy(() -> provider.supports(COMIC_URL)).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void discoverSources_reusesTheSourcesWhileTheTtlHasNotPassed() {
        // Propósito: la lista de fuentes de Suwayomi se guarda en caché durante el TTL.
        // Flujo:
        // 1. Con un TTL largo, se piden las fuentes dos veces seguidas.
        // 2. Solo se espera una petición a Suwayomi: la segunda sale de la caché.
        expectSources();
        SuwayomiComicReadingProvider provider = provider();

        provider.discoverSources();
        provider.discoverSources();

        server.verify();
    }

    @Test
    void discoverSources_fetchesTheSourcesAgainOnceTheTtlHasPassed() {
        // Propósito: la lista de fuentes de Suwayomi se vuelve a pedir cuando la caché ha caducado.
        // Flujo:
        // 1. Con un TTL de cero, la caché caduca nada más guardarse.
        // 2. Se piden las fuentes dos veces seguidas y se esperan dos peticiones a Suwayomi.
        server.expect(ExpectedCount.times(2), requestTo(GRAPHQL_URL))
                .andRespond(withSuccess(SOURCES_JSON, MediaType.APPLICATION_JSON));
        SuwayomiComicReadingProvider provider = provider(Duration.ZERO, registryWith(RateLimiterConfig.ofDefaults()));

        provider.discoverSources();
        provider.discoverSources();

        server.verify();
    }

    @Test
    void fetch_byExternalIdAsksSuwayomiForThatWorkAndMapsTheDetails() {
        expectMangaAndChapters();

        Optional<ComicReadingEntryDetails> details = provider().fetch(LINKED_SOURCE,
                new ComicReadingEntryTarget(null, "2"));

        assertThat(details).isPresent();
        assertThat(details.get().externalId()).isEqualTo("2");
        assertThat(details.get().url()).isEqualTo(COMIC_URL);
        assertThat(details.get().title()).isEqualTo("Solo Leveling");
        assertThat(details.get().availableChapters()).isEqualTo(2);
        assertThat(details.get().latestChapterAt()).isEqualTo(Instant.ofEpochMilli(1710000000000L));
        server.verify();
    }

    @Test
    void fetch_byUrlFirstLooksTheWorkUpAmongThoseSuwayomiHasStored() {
        // Propósito: una URL se traduce al id de Suwayomi buscando entre las obras que ya tiene guardadas.
        // Flujo:
        // 1. Se pide la obra por su URL pública.
        // 2. El provider consulta las obras guardadas de la fuente y obtiene el id 2.
        // 3. Con ese id pide la obra y sus capítulos.
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(jsonPath("$.variables.sourceId").value(SUWAYOMI_SOURCE_ID))
                .andExpect(jsonPath("$.variables.realUrl").value(COMIC_URL))
                .andRespond(withSuccess("""
                        { "data": { "mangas": { "nodes": [ { "id": 2, "title": "Solo Leveling", "realUrl": "%s" } ] } } }
                        """.formatted(COMIC_URL), MediaType.APPLICATION_JSON));
        expectMangaAndChapters();

        Optional<ComicReadingEntryDetails> details = provider().fetch(LINKED_SOURCE,
                new ComicReadingEntryTarget(COMIC_URL, null));

        assertThat(details).isPresent();
        server.verify();
    }

    @Test
    void fetch_byUrlWithoutTrailingSlashFindsTheWorkStoredWithIt() {
        // Propósito: una URL sin barra final (así las guarda el normalizador) encuentra la obra que Suwayomi guarda con ella.
        // Flujo:
        // 1. Se pide la obra con la URL sin la barra final.
        // 2. La primera consulta, con la URL tal cual, no encuentra nada.
        // 3. La segunda, con la barra final añadida, encuentra la obra con id 2.
        // 4. Con ese id pide la obra y sus capítulos.
        String withoutSlash = COMIC_URL.substring(0, COMIC_URL.length() - 1);
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(jsonPath("$.variables.realUrl").value(withoutSlash))
                .andRespond(withSuccess("{ \"data\": { \"mangas\": { \"nodes\": [] } } }",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(jsonPath("$.variables.realUrl").value(COMIC_URL))
                .andRespond(withSuccess("""
                        { "data": { "mangas": { "nodes": [ { "id": 2, "title": "Solo Leveling", "realUrl": "%s" } ] } } }
                        """.formatted(COMIC_URL), MediaType.APPLICATION_JSON));
        expectMangaAndChapters();

        Optional<ComicReadingEntryDetails> details = provider().fetch(LINKED_SOURCE,
                new ComicReadingEntryTarget(withoutSlash, null));

        assertThat(details).isPresent();
        server.verify();
    }

    @Test
    void fetch_throwsInsteadOfReturningEmptyWhenSuwayomiDoesNotKnowTheWork() {
        // Propósito: si Suwayomi no conoce la obra en ninguna de las dos formas de la URL, se lanza en vez de devolver vacío.
        // Suwayomi solo conoce las obras ya guardadas, así que no encontrarla no prueba que el sitio no la tenga.
        server.expect(ExpectedCount.times(2), requestTo(GRAPHQL_URL))
                .andRespond(withSuccess("{ \"data\": { \"mangas\": { \"nodes\": [] } } }",
                        MediaType.APPLICATION_JSON));

        SuwayomiComicReadingProvider provider = provider();
        ComicReadingEntryTarget entry = new ComicReadingEntryTarget(COMIC_URL, null);

        assertThatThrownBy(() -> provider.fetch(LINKED_SOURCE, entry)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fetch_throwsWhenSuwayomiHasNoPublicUrlForTheWork() {
        server.expect(requestTo(GRAPHQL_URL))
                .andRespond(withSuccess("""
                        { "data": { "fetchMangaAndChapters": {
                          "manga": { "id": 2, "title": "Solo Leveling", "realUrl": null }, "chapters": []
                        } } }
                        """, MediaType.APPLICATION_JSON));

        SuwayomiComicReadingProvider provider = provider();
        ComicReadingEntryTarget entry = new ComicReadingEntryTarget(null, "2");

        assertThatThrownBy(() -> provider.fetch(LINKED_SOURCE, entry)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fetch_throwsWhenTheExternalIdIsNotANumber() {
        SuwayomiComicReadingProvider provider = provider();
        ComicReadingEntryTarget entry = new ComicReadingEntryTarget(null, "abc");

        assertThatThrownBy(() -> provider.fetch(LINKED_SOURCE, entry)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fetch_resolvesTheSuwayomiSourceFromTheSourceUrlWhenItIsNotLinkedYet() {
        // Propósito: una fuente sin vincular se identifica por el host de su URL entre las fuentes de Suwayomi.
        // Flujo:
        // 1. El provider pide la lista de fuentes y encuentra la que tiene ese host.
        // 2. Con el id de esa fuente pide la obra.
        expectSources();
        expectMangaAndChapters();

        Optional<ComicReadingEntryDetails> details = provider().fetch(UNLINKED_SOURCE,
                new ComicReadingEntryTarget(null, "2"));

        assertThat(details).isPresent();
        server.verify();
    }

    @Test
    void fetch_throwsWhenNoSuwayomiSourceMatchesTheSourceUrl() {
        expectSources();

        SuwayomiComicReadingProvider provider = provider();
        ComicReadingSourceTarget unknownSource = new ComicReadingSourceTarget("https://other.example", null);
        ComicReadingEntryTarget entry = new ComicReadingEntryTarget(null, "2");

        assertThatThrownBy(() -> provider.fetch(unknownSource, entry)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void search_returnsTheFirstPageMappedAndLeavesTheUrlNullWhenSuwayomiHasNone() {
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(jsonPath("$.variables.source").value(SUWAYOMI_SOURCE_ID))
                .andExpect(jsonPath("$.variables.query").value("solo"))
                .andRespond(withSuccess("""
                        { "data": { "fetchSourceManga": { "hasNextPage": true, "mangas": [
                          { "id": 1, "title": "Level Up Alone", "realUrl": null },
                          { "id": 2, "title": "Solo Leveling", "realUrl": "%s" }
                        ] } } }
                        """.formatted(COMIC_URL), MediaType.APPLICATION_JSON));

        List<ComicReadingSearchResult> results = provider().search(LINKED_SOURCE, "solo");

        assertThat(results).containsExactly(new ComicReadingSearchResult("1", "Level Up Alone", null, null),
                new ComicReadingSearchResult("2", "Solo Leveling", COMIC_URL, null));
        server.verify();
    }

    @Test
    void search_limitsEachSuwayomiSourceOnItsOwn() {
        // Propósito: cada fuente de Suwayomi tiene su propio limitador, así que agotar el de una no frena a las demás.
        // Flujo:
        // 1. Se configura un limitador que solo permite una llamada y no espera.
        // 2. Se busca en la fuente A: pasa.
        // 3. Se busca otra vez en la fuente A: se rechaza con RequestNotPermitted sin llegar a la red.
        // 4. Se busca en la fuente B: pasa, porque tiene su propio limitador.
        RateLimiterConfig oneCall = RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofHours(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        SuwayomiComicReadingProvider provider = provider(Duration.ofMinutes(10), registryWith(oneCall));
        String emptyPage = "{ \"data\": { \"fetchSourceManga\": { \"hasNextPage\": false, \"mangas\": [] } } }";
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(jsonPath("$.variables.source").value(SUWAYOMI_SOURCE_ID))
                .andRespond(withSuccess(emptyPage, MediaType.APPLICATION_JSON));
        server.expect(requestTo(GRAPHQL_URL))
                .andExpect(jsonPath("$.variables.source").value(OTHER_SUWAYOMI_SOURCE_ID))
                .andRespond(withSuccess(emptyPage, MediaType.APPLICATION_JSON));

        provider.search(LINKED_SOURCE, "solo");
        assertThatThrownBy(() -> provider.search(LINKED_SOURCE, "solo")).isInstanceOf(RequestNotPermitted.class);
        provider.search(new ComicReadingSourceTarget("https://manhwaweb.com", OTHER_SUWAYOMI_SOURCE_ID), "solo");

        server.verify();
    }
}
