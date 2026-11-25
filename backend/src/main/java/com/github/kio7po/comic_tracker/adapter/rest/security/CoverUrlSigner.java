package com.github.kio7po.comic_tracker.adapter.rest.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Builds and checks the addresses of the covers the backend serves itself (the ones a provider can't give as a public
 * address). The address carries a signed token, so the endpoint only serves what a search handed out. It has its own
 * key and isn't built from the access token's encoder: a cover token must never be accepted as a credential.
 */
@Component
public class CoverUrlSigner {

    private static final String COVERS_PATH = "/api/covers/";
    private static final String PROVIDER_KEY_CLAIM = "providerKey";
    private static final String EXTERNAL_COVER_ID_CLAIM = "externalCoverId";

    private final NimbusJwtEncoder jwtEncoder;
    private final NimbusJwtDecoder jwtDecoder;
    private final Duration minValidity;
    private final Clock clock;

    @Autowired
    public CoverUrlSigner(@Value("${cover.url-sign-key}") String signKey,
            @Value("${cover.url-min-validity:6h}") Duration minValidity) {
        this(signKey, minValidity, Clock.systemUTC());
    }

    CoverUrlSigner(String signKey, Duration minValidity, Clock clock) {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(signKey), "HmacSHA256");
        this.jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        // El validador de caducidad usa el mismo reloj que el firmador y no admite margen: la caducidad es exacta.
        JwtTimestampValidator expirationValidator = new JwtTimestampValidator(Duration.ZERO);
        expirationValidator.setClock(clock);
        this.jwtDecoder.setJwtValidator(expirationValidator);
        this.minValidity = minValidity;
        this.clock = clock;
    }

    /**
     * @return the address, relative to the API, a browser can load the cover from.
     */
    public String signedCoverUrl(String providerKey, String externalCoverId) {
        // Sin iat ni jti: así la misma portada produce el mismo token dentro de una ventana y el navegador puede
        // reutilizar su caché.
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .claim(PROVIDER_KEY_CLAIM, providerKey)
                .claim(EXTERNAL_COVER_ID_CLAIM, externalCoverId)
                .expiresAt(expiration())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return COVERS_PATH + jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * @return the cover the token stands for, or empty if it isn't one of ours, was altered or has expired.
     */
    public Optional<CoverAddress> verify(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String providerKey = jwt.getClaimAsString(PROVIDER_KEY_CLAIM);
            String externalCoverId = jwt.getClaimAsString(EXTERNAL_COVER_ID_CLAIM);
            if (providerKey == null || externalCoverId == null || jwt.getExpiresAt() == null) {
                return Optional.empty();
            }
            return Optional.of(new CoverAddress(providerKey, externalCoverId));
        } catch (JwtException e) {
            return Optional.empty();
        }
    }

    /**
     * @return how long a signed address is valid at least; a browser can keep the image that long.
     */
    public Duration minValidity() {
        return minValidity;
    }

    // El tiempo se parte en ventanas de la duración mínima de validez, y todo lo firmado dentro de una ventana
    // caduca al final de la siguiente. Así una dirección dura entre una y dos ventanas (nunca menos de la mínima,
    // aunque se firme justo al final de una) y es la misma durante toda la ventana.
    private Instant expiration() {
        long windowSeconds = minValidity.toSeconds();
        long currentWindowStart = clock.instant().getEpochSecond() / windowSeconds * windowSeconds;
        return Instant.ofEpochSecond(currentWindowStart + 2 * windowSeconds);
    }

    /**
     * @param providerKey the provider that serves the cover.
     * @param externalCoverId the cover's identifier in that provider.
     */
    public record CoverAddress(String providerKey, String externalCoverId) {
    }

}
