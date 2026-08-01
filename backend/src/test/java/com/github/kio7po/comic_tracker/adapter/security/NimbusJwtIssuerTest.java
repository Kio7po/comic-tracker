package com.github.kio7po.comic_tracker.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.UserRole;

class NimbusJwtIssuerTest {

    private static final String ISSUER = "comic-tracker-test";
    private static final String AUDIENCE = "comic-tracker-test";
    private static final long EXPIRATION_MINUTES = 15;

    private NimbusJwtIssuer jwtIssuer;
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        SecretKey secretKey = new SecretKeySpec(
                "test-only-hs256-signing-key-32b!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();

        jwtIssuer = new NimbusJwtIssuer(jwtEncoder, ISSUER, AUDIENCE, EXPIRATION_MINUTES);
        jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private static User user() {
        User user = new User();
        user.setUsername("testuser");
        user.setRole(UserRole.USER);
        return user;
    }

    @Test
    void issueProducesAJwtWithTheExpectedClaims() {
        // JWT NumericDate (RFC 7519 §2) has whole-second precision, so truncate before
        // comparing against the decoded claim to avoid a spurious sub-second mismatch.
        Instant before = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        String token = jwtIssuer.issue(user());
        Jwt decoded = jwtDecoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("testuser");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo(ISSUER);
        assertThat(decoded.getAudience()).containsExactly(AUDIENCE);
        assertThat(decoded.getClaimAsString("role")).isEqualTo("USER");
        assertThat(decoded.getIssuedAt()).isNotNull().isAfterOrEqualTo(before);
        assertThat(decoded.getExpiresAt()).isEqualTo(decoded.getIssuedAt().plusSeconds(EXPIRATION_MINUTES * 60));
    }

    @Test
    void issueEmbedsTheUserRoleInTheToken() {
        User admin = user();
        admin.setRole(UserRole.ADMIN);

        Jwt decoded = jwtDecoder.decode(jwtIssuer.issue(admin));

        assertThat(decoded.getClaimAsString("role")).isEqualTo("ADMIN");
    }

}
