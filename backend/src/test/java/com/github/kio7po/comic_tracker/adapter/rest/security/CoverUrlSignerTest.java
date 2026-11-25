package com.github.kio7po.comic_tracker.adapter.rest.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.kio7po.comic_tracker.adapter.rest.security.CoverUrlSigner.CoverAddress;

class CoverUrlSignerTest {

    private static final String KEY = Base64.getEncoder()
            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final String OTHER_KEY = Base64.getEncoder()
            .encodeToString("fedcba9876543210fedcba9876543210".getBytes(StandardCharsets.UTF_8));
    private static final Duration MIN_VALIDITY = Duration.ofHours(6);
    // Las ventanas son múltiplos de 6 h desde la época, y medianoche UTC lo es: este instante abre una ventana.
    private static final Instant WINDOW_START = Instant.parse("2026-11-23T00:00:00Z");
    private static final String COVERS_PATH = "/api/covers/";

    private MutableClock clock;
    private CoverUrlSigner signer;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(WINDOW_START);
        signer = new CoverUrlSigner(KEY, MIN_VALIDITY, clock);
    }

    private static String tokenOf(String signedCoverUrl) {
        return signedCoverUrl.substring(COVERS_PATH.length());
    }

    @Test
    void signedCoverUrlIsUnderTheCoversPathAndVerifiesBackToTheSameCover() {
        String url = signer.signedCoverUrl("suwayomi", "731");

        assertThat(url).startsWith(COVERS_PATH);
        assertThat(signer.verify(tokenOf(url))).contains(new CoverAddress("suwayomi", "731"));
    }

    @Test
    void signedCoverUrlIsTheSameForTheSameCoverWithinAWindow() {
        // Propósito: dentro de una ventana la misma portada tiene siempre la misma dirección, para que el
        // navegador pueda reutilizar su caché entre búsquedas.
        //
        // Flujo: se firma la portada al abrirse una ventana y de nuevo casi al final de esa misma ventana; las dos
        // direcciones son idénticas.
        String first = signer.signedCoverUrl("suwayomi", "731");
        clock.setNow(WINDOW_START.plus(MIN_VALIDITY).minusSeconds(1));

        assertThat(signer.signedCoverUrl("suwayomi", "731")).isEqualTo(first);
    }

    @Test
    void signedCoverUrlChangesWhenTheWindowChanges() {
        String first = signer.signedCoverUrl("suwayomi", "731");
        clock.setNow(WINDOW_START.plus(MIN_VALIDITY));

        assertThat(signer.signedCoverUrl("suwayomi", "731")).isNotEqualTo(first);
    }

    @Test
    void signedCoverUrlDiffersBetweenCovers() {
        assertThat(signer.signedCoverUrl("suwayomi", "731")).isNotEqualTo(signer.signedCoverUrl("suwayomi", "732"));
        assertThat(signer.signedCoverUrl("suwayomi", "731")).isNotEqualTo(signer.signedCoverUrl("other", "731"));
    }

    @Test
    void verifyAcceptsTheAddressForAtLeastTheMinimumValidityEvenWhenItWasSignedAtTheEndOfAWindow() {
        // Propósito: una dirección firmada justo antes de que acabe una ventana no caduca antes de la validez
        // mínima, para que las portadas que se piden perezosamente al hacer scroll no den 403.
        //
        // Flujo: se firma un segundo antes de que cierre la ventana. Pasada la validez mínima completa desde ese
        // momento, la dirección sigue siendo válida (caduca al final de la ventana siguiente).
        clock.setNow(WINDOW_START.plus(MIN_VALIDITY).minusSeconds(1));
        String token = tokenOf(signer.signedCoverUrl("suwayomi", "731"));

        clock.setNow(WINDOW_START.plus(MIN_VALIDITY).minusSeconds(1).plus(MIN_VALIDITY));

        assertThat(signer.verify(token)).contains(new CoverAddress("suwayomi", "731"));
    }

    @Test
    void verifyRejectsAnExpiredAddress() {
        String token = tokenOf(signer.signedCoverUrl("suwayomi", "731"));

        // La dirección caduca al final de la ventana siguiente: dos validez mínimas después de abrirse la ventana.
        clock.setNow(WINDOW_START.plus(MIN_VALIDITY.multipliedBy(2)).plusSeconds(1));

        assertThat(signer.verify(token)).isEmpty();
    }

    @Test
    void verifyRejectsAnAddressSignedWithAnotherKey() {
        String token = tokenOf(new CoverUrlSigner(OTHER_KEY, MIN_VALIDITY, clock).signedCoverUrl("suwayomi", "731"));

        assertThat(signer.verify(token)).isEmpty();
    }

    @Test
    void verifyRejectsATokenWhoseSignatureWasAltered() {
        String[] parts = tokenOf(signer.signedCoverUrl("suwayomi", "731")).split("\\.");
        String altered = parts[0] + "." + parts[1] + "." + "A".repeat(parts[2].length());

        assertThat(signer.verify(altered)).isEmpty();
    }

    @Test
    void verifyRejectsATokenWhosePayloadWasAlteredKeepingTheSignature() {
        // Propósito: cambiar a qué portada apunta un token sin poder firmarlo no sirve de nada.
        //
        // Flujo: se firma una portada, se decodifica el cuerpo del token, se cambia el id de la portada por otro y
        // se vuelve a codificar dejando la firma original. La verificación lo rechaza.
        String[] parts = tokenOf(signer.signedCoverUrl("suwayomi", "731")).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String alteredPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.replace("731", "999").getBytes(StandardCharsets.UTF_8));

        assertThat(payload).contains("731");
        assertThat(signer.verify(parts[0] + "." + alteredPayload + "." + parts[2])).isEmpty();
    }

    @ParameterizedTest(name = "token: [{0}]")
    @ValueSource(strings = { "", "not-a-token", "a.b.c" })
    void verifyRejectsSomethingThatIsNotAToken(String token) {
        assertThat(signer.verify(token)).isEmpty();
    }

    @Test
    void minValidityIsTheConfiguredOne() {
        assertThat(signer.minValidity()).isEqualTo(MIN_VALIDITY);
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void setNow(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

}
