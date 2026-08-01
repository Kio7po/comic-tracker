package com.github.kio7po.comic_tracker.adapter.rest.security;

import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.sign-key}") String signKey, @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience) {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(signKey), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();

        // createDefaultWithValidators keeps the built-in checks and adds ours on top
        // issuer/audience are NOT validated by default, only signature + exp/nbf.
        OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator(issuer);
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(audience);
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithValidators(List.of(issuerValidator, audienceValidator)));

        return decoder;
    }

    // Maps the "role" claim to a ROLE_-prefixed GrantedAuthority so hasRole(...) works
    // Spring's default converter only knows about "scope"/"scp" claims.
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

}
