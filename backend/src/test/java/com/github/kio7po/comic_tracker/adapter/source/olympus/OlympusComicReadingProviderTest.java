package com.github.kio7po.comic_tracker.adapter.source.olympus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryTarget;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSearchResult;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceDescriptor;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceTarget;

class OlympusComicReadingProviderTest {

    private static final String BASE_URL = "https://olympusxyz.com";
    private static final String PANEL_BASE_URL = "https://panel.olympusxyz.com";
    private static final Duration SERIES_LIST_TTL = Duration.ofMinutes(10);
    // Olympus sirve un único sitio, así que no usa la fuente que recibe: todos los tests le pasan la misma.
    private static final ComicReadingSourceTarget SOURCE = new ComicReadingSourceTarget(BASE_URL, null);

    private MockRestServiceServer server;
    private OlympusComicReadingProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new OlympusComicReadingProvider(builder, BASE_URL, SERIES_LIST_TTL,
                RateLimiterRegistry.ofDefaults());
    }

    private static Optional<ComicReadingEntryDetails> fetch(OlympusComicReadingProvider provider, String url,
            String externalId) {
        return provider.fetch(SOURCE, new ComicReadingEntryTarget(url, externalId));
    }

    private static List<ComicReadingSearchResult> search(OlympusComicReadingProvider provider, String keywords) {
        return provider.search(SOURCE, keywords);
    }

    private static String mangaDetailJson(String name) {
        return """
                {
                  "data": { "id": 42, "name": "%s", "slug": "berserk", "type": "comic" }
                }
                """.formatted(name);
    }

    private static String chaptersJson(int total, String... publishedAt) {
        String data = java.util.Arrays.stream(publishedAt)
                .map("{ \"published_at\": \"%s\" }"::formatted)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return """
                {
                  "data": [%s],
                  "meta": { "total": %d }
                }
                """.formatted(data, total);
    }

    private static String seriesListJson() {
        return """
                {
                  "data": [
                    { "id": 1, "name": "The Sword King", "slug": "sword-king", "type": "comic" },
                    { "id": 2, "name": "The Sword King Light Novel", "slug": "sword-king-novel", "type": "novel" },
                    { "id": 3, "name": "One Piece", "slug": "one-piece", "type": "comic" }
                  ]
                }
                """;
    }

    @Test
    void key_isOlympus() {
        assertThat(provider.key()).isEqualTo("olympus");
    }

    @Test
    void discoverSources_returnsTheConfiguredSiteWithAFixedIdentifier() {
        assertThat(provider.discoverSources())
                .containsExactly(new ComicReadingSourceDescriptor("olympus", "Olympus Scanlation", BASE_URL));
    }

    @Test
    void supports_trueForAUrlOnTheConfiguredHost() {
        assertThat(provider.supports("https://olympusxyz.com/series/comic-berserk")).isTrue();
    }

    @Test
    void supports_isCaseInsensitiveOnTheHost() {
        assertThat(provider.supports("https://OlympusXYZ.com/series/comic-berserk")).isTrue();
    }

    @Test
    void supports_falseForADifferentHost() {
        assertThat(provider.supports("https://example.com/series/comic-berserk")).isFalse();
    }

    @Test
    void supports_falseForAMalformedUrl() {
        assertThat(provider.supports("not a url")).isFalse();
    }

    @Test
    void fetch_requestsTheMangaDetailAndChaptersEndpointsForTheUrlsSlug() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(chaptersJson(374, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        fetch(provider, "https://olympusxyz.com/series/comic-berserk", null);

        server.verify();
    }

    @Test
    void fetch_mapsASuccessfulResponseToDetails() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(374, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        Optional<ComicReadingEntryDetails> result = fetch(provider, "https://olympusxyz.com/series/comic-berserk", null);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("Berserk");
        assertThat(result.get().availableChapters()).isEqualTo(374);
        assertThat(result.get().latestChapterAt()).isEqualTo(Instant.parse("2024-06-01T10:00:00.000000Z"));
    }

    @Test
    void fetch_returnsTheExternalIdAndTheCanonicalUrlFromTheDetailResponse() {
        // Propósito: la URL que devuelve el provider es la canónica que él construye, no la que pegó el
        // usuario. Aquí se pega una URL con un segmento de más ("/some-extra-segment"), que debe
        // desaparecer en la URL devuelta. El id sale del detalle ("id": 42 en el JSON de prueba).

        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(1, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        Optional<ComicReadingEntryDetails> result = fetch(provider,
                "https://olympusxyz.com/series/comic-berserk/some-extra-segment", null);

        assertThat(result).isPresent();
        assertThat(result.get().externalId()).isEqualTo("42");
        assertThat(result.get().url()).isEqualTo(BASE_URL + "/series/comic-berserk");
    }

    @Test
    void fetch_resolvesTheCurrentSlugFromTheExternalIdThroughTheSeriesList() {
        // Propósito: con externalId, el provider obtiene el slug actual de la obra a partir de su id en
        // lugar de fiarse del slug de la URL. Olympus cambia los slugs de vez en cuando; el id no cambia.
        //
        // Flujo (el servidor simulado exige las peticiones en este orden):
        //  1. fetch con una URL de slug viejo ("an-old-slug") y el externalId "3".
        //  2. El provider pide la lista de series y busca el id 3: es "One Piece", slug actual "one-piece".
        //  3. Con ese slug pide el detalle y los capítulos. Si hubiera usado el slug de la URL, habría
        //     pedido "an-old-slug" y el test fallaría.
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/series/one-piece?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("One Piece"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/one-piece/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(1, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        Optional<ComicReadingEntryDetails> result = fetch(provider,
                "https://olympusxyz.com/series/comic-an-old-slug", "3");

        assertThat(result).isPresent();
        server.verify();
    }

    @Test
    void fetch_fallsBackToTheUrlSlugWhenTheExternalIdIsNotInTheSeriesList() {
        // Propósito: si el id guardado ya no aparece en la lista de series (por ejemplo, la serie se
        // borró), el provider usa el slug de la URL como último recurso.
        //
        // Flujo: fetch con el externalId "999" (que no existe en la lista) y una URL con slug "berserk":
        //  1. Pide la lista y no encuentra el id 999.
        //  2. Recurre al slug de la URL y pide el detalle y los capítulos de "berserk".
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(1, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        Optional<ComicReadingEntryDetails> result = fetch(provider,
                "https://olympusxyz.com/series/comic-berserk", "999");

        assertThat(result).isPresent();
        server.verify();
    }

    @Test
    void fetch_resolvesTheWorkFromTheExternalIdAloneWhenThereIsNoUrl() {
        // Propósito: se puede pedir una obra solo por su id, sin URL; es el caso de una obra elegida en una
        // búsqueda, donde el sitio no da la dirección hasta que se hidrata.
        //
        // Flujo (el servidor simulado exige las peticiones en este orden):
        //  1. fetch con url = null y el externalId "3".
        //  2. El provider pide la lista de series y busca el id 3: "One Piece", slug "one-piece".
        //  3. Con ese slug pide el detalle y los capítulos.
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/series/one-piece?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("One Piece"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/one-piece/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(1, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        Optional<ComicReadingEntryDetails> result = fetch(provider, null, "3");

        assertThat(result).isPresent();
        server.verify();
    }

    @Test
    void fetch_returnsEmptyWhenThereIsNoUrlAndTheExternalIdIsNotInTheSeriesList() {
        // Propósito: sin URL a la que recurrir, un id que ya no está en la lista de series significa que
        // la obra no se puede localizar, y el resultado es vacío sin pedir ningún detalle.
        //
        // Flujo: fetch con url = null y el externalId "999". El provider pide la lista, no encuentra el id
        // y termina; no se declara ninguna otra petición, así que pedir el detalle haría fallar el test.
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        Optional<ComicReadingEntryDetails> result = fetch(provider, null, "999");

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void fetch_doesNotResolveAnExternalIdThatBelongsToASeriesOfAnotherType() {
        // Propósito: al buscar el slug por id solo cuentan las series de tipo "comic" (el tipo que se
        // pide en el detalle); una novela con ese mismo id no aporta su slug.
        //
        // Flujo: en la lista de prueba el id 2 es una novela ligera ("sword-king-novel").
        //  1. fetch con el externalId "2" y una URL con slug "berserk".
        //  2. El provider ignora la novela, no encuentra ningún comic con id 2 y recurre al slug de la
        //     URL ("berserk"). Si hubiera usado el slug de la novela, habría pedido otra ruta y fallaría.
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(1, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        fetch(provider, "https://olympusxyz.com/series/comic-berserk", "2");

        server.verify();
    }

    @Test
    void fetch_extractsTheSlugFromAUrlWithExtraPathSegments() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(1, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));

        fetch(provider, "https://olympusxyz.com/series/comic-berserk/some-extra-segment", null);

        server.verify();
    }

    @Test
    void fetch_returnsEmptyWithoutCallingTheApiWhenTheUrlHasNoRecognizableSlug() {
        // No server.expect(...) set up on purpose: the request must never reach the network -
        // MockRestServiceServer fails the test itself if it does.
        Optional<ComicReadingEntryDetails> result = fetch(provider, "https://olympusxyz.com/other/path", null);

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void fetch_returnsEmptyWhenTheMangaDetailEndpointRespondsNotFound() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withResourceNotFound());

        Optional<ComicReadingEntryDetails> result = fetch(provider, "https://olympusxyz.com/series/comic-berserk", null);

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void fetch_propagatesTheExceptionWhenTheMangaDetailEndpointRespondsServerError() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> fetch(provider, "https://olympusxyz.com/series/comic-berserk", null))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void fetch_handlesAnEmptyChapterListByLeavingLatestChapterAtNull() {
        server.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(0), MediaType.APPLICATION_JSON));

        Optional<ComicReadingEntryDetails> result = fetch(provider, "https://olympusxyz.com/series/comic-berserk", null);

        assertThat(result).isPresent();
        assertThat(result.get().availableChapters()).isZero();
        assertThat(result.get().latestChapterAt()).isNull();
    }

    @Test
    void search_requestsTheSeriesListEndpoint() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        search(provider, "berserk");

        server.verify();
    }

    @Test
    void search_filtersByTypeComicAndCaseInsensitiveNameMatch() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        List<ComicReadingSearchResult> results = search(provider, "SWORD");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("The Sword King");
        assertThat(results.get(0).url()).isEqualTo(BASE_URL + "/series/comic-sword-king");
    }

    @Test
    void search_matchesEveryWordOfTheQueryRegardlessOfOrder() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        // Reversed compared to the actual title ("The Sword King"), and skipping "the" entirely -
        // every word still has to match, but not in order or exhaustively.
        List<ComicReadingSearchResult> results = search(provider, "king sword");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("The Sword King");
    }

    @Test
    void search_excludesResultsMissingAnyQueryWord() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        List<ComicReadingSearchResult> results = search(provider, "sword lord");

        assertThat(results).isEmpty();
    }

    @Test
    void search_returnsAnEmptyListWhenDataIsNull() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess("{\"data\":null}", MediaType.APPLICATION_JSON));

        List<ComicReadingSearchResult> results = search(provider, "berserk");

        assertThat(results).isEmpty();
    }

    @Test
    void search_propagatesTheExceptionWhenTheApiRespondsServerError() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> search(provider, "berserk"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void search_includesTheExternalIdOfEachResult() {
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        List<ComicReadingSearchResult> results = search(provider, "sword");

        assertThat(results).extracting(ComicReadingSearchResult::externalId).containsExactly("1");
    }

    @Test
    void search_reusesTheSeriesListWhileItIsWithinTheTtl() {
        // Propósito: la lista completa de series (la usan la búsqueda y la resolución de id a slug)
        // se pide a Olympus una sola vez y se reutiliza mientras no pase el TTL.
        //
        // Flujo:
        //  1. Se declara UNA sola petición esperada a la lista. Si llegara una segunda, el servidor
        //     simulado no tiene nada declarado para ella y hace fallar el test.
        //  2. La primera búsqueda pide la lista a la red y la guarda en caché.
        //  3. La segunda búsqueda (con otra palabra) se responde desde la caché, sin red. Aun así
        //     devuelve el resultado correcto, lo que demuestra que filtró sobre la lista guardada.
        server.expect(requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        search(provider, "sword");
        List<ComicReadingSearchResult> second = search(provider, "piece");

        assertThat(second).extracting(ComicReadingSearchResult::title).containsExactly("One Piece");
        server.verify();
    }

    @Test
    void search_fetchesTheSeriesListAgainOnceTheTtlHasExpired() {
        // Propósito: pasado el TTL, la lista de series guardada se descarta y se vuelve a pedir a Olympus.
        //
        // Flujo:
        //  1. Se crea un provider con TTL = 0, así que la lista guardada caduca al instante.
        //  2. Se declara que se esperan DOS peticiones a la lista (ExpectedCount.twice()).
        //  3. Dos búsquedas seguidas van las dos a la red; verify() falla si solo se hizo una.
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer expiringServer = MockRestServiceServer.bindTo(builder).build();
        OlympusComicReadingProvider expiringProvider = new OlympusComicReadingProvider(builder, BASE_URL,
                Duration.ZERO, RateLimiterRegistry.ofDefaults());
        expiringServer.expect(ExpectedCount.twice(), requestTo(BASE_URL + "/api/series/list"))
                .andRespond(withSuccess(seriesListJson(), MediaType.APPLICATION_JSON));

        search(expiringProvider, "sword");
        search(expiringProvider, "sword");

        expiringServer.verify();
    }

    @Test
    void search_isRejectedWithoutCallingTheApiWhenTheBaseLimiterIsExhausted() {
        // Propósito: cuando el limitador de velocidad de Olympus no tiene cupo, la búsqueda falla con
        // RequestNotPermitted sin llegar a hacer la petición a la red.
        //
        // Flujo:
        //  1. El limitador "olympus-base" solo tiene 1 permiso por minuto y el test lo gasta antes de
        //     buscar (acquirePermission), así que ya está agotado.
        //  2. Al buscar, el provider intenta pedir la lista, el limitador la rechaza y se lanza
        //     RequestNotPermitted, con el nombre del limitador que la rechazó.
        //  3. No se declara ninguna petición esperada: si el provider llegara a la red, el servidor
        //     simulado haría fallar el test.
        //
        // El permiso se gasta a mano y no con una primera búsqueda porque la lista queda en caché tras
        // una búsqueda correcta: una segunda búsqueda ya no pasaría por el limitador y no probaría nada.
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer limitedServer = MockRestServiceServer.bindTo(builder).build();
        RateLimiterRegistry registry = RateLimiterRegistry.custom().build();
        registry.rateLimiter("olympus-base", exhaustedAfterOneCall()).acquirePermission();
        registry.rateLimiter("olympus-panel", RateLimiterConfig.ofDefaults());
        OlympusComicReadingProvider limitedProvider = new OlympusComicReadingProvider(builder, BASE_URL,
                SERIES_LIST_TTL, registry);

        assertThatThrownBy(() -> search(limitedProvider, "sword"))
                .isInstanceOf(RequestNotPermitted.class)
                .extracting(exception -> ((RequestNotPermitted) exception).getCausingRateLimiterName())
                .isEqualTo("olympus-base");
        limitedServer.verify();
    }

    private static Stream<Arguments> exhaustedLimiters() {
        return Stream.of(
                Arguments.of("olympus-base exhausted", "olympus-base"),
                Arguments.of("olympus-panel exhausted", "olympus-panel"));
    }

    // A single permit that never refreshes within the test's lifetime, and never waits for one either -
    // the first call consumes it, any further call is rejected immediately instead of blocking.
    private static RateLimiterConfig exhaustedAfterOneCall() {
        return RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
    }

    private static OlympusComicReadingProvider providerWithExhaustedLimiter(RestClient.Builder builder,
            String exhaustedLimiterName) {
        RateLimiterConfig baseConfig = "olympus-base".equals(exhaustedLimiterName)
                ? exhaustedAfterOneCall() : RateLimiterConfig.ofDefaults();
        RateLimiterConfig panelConfig = "olympus-panel".equals(exhaustedLimiterName)
                ? exhaustedAfterOneCall() : RateLimiterConfig.ofDefaults();
        // RateLimiterRegistry.of(Map<String, RateLimiterConfig>) does NOT register these as named
        // instances, registry.rateLimiter(name, config) is what actually does.
        RateLimiterRegistry registry = RateLimiterRegistry.custom().build();
        registry.rateLimiter("olympus-base", baseConfig);
        registry.rateLimiter("olympus-panel", panelConfig);
        return new OlympusComicReadingProvider(builder, BASE_URL, SERIES_LIST_TTL, registry);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("exhaustedLimiters")
    void fetch_isRejectedWithoutCallingTheApiWhenALimitIsExhausted(String caseName, String exhaustedLimiterName) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer limitedServer = MockRestServiceServer.bindTo(builder).build();
        OlympusComicReadingProvider limitedProvider = providerWithExhaustedLimiter(builder, exhaustedLimiterName);

        // MockRestServiceServer expects requests in declaration order, so this has to match the
        // actual interleaving: manga then chapters for the first (successful) fetch() always: base
        // exhausted stops the second fetch() before it reaches fetchManga at all; panel exhausted
        // lets fetchManga succeed again but rejects fetchChapters before it reaches the network.
        limitedServer.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        limitedServer.expect(requestTo(PANEL_BASE_URL + "/api/series/berserk/chapters?page=1&direction=desc&type=comic"))
                .andRespond(withSuccess(chaptersJson(1, "2024-06-01T10:00:00.000000Z"), MediaType.APPLICATION_JSON));
        if (!"olympus-base".equals(exhaustedLimiterName)) {
            limitedServer.expect(requestTo(BASE_URL + "/api/series/berserk?type=comic"))
                    .andRespond(withSuccess(mangaDetailJson("Berserk"), MediaType.APPLICATION_JSON));
        }

        fetch(limitedProvider, "https://olympusxyz.com/series/comic-berserk", null);

        assertThatThrownBy(() -> fetch(limitedProvider, "https://olympusxyz.com/series/comic-berserk", null))
                .isInstanceOf(RequestNotPermitted.class)
                .extracting(exception -> ((RequestNotPermitted) exception).getCausingRateLimiterName())
                .isEqualTo(exhaustedLimiterName);
        limitedServer.verify();
    }
}
