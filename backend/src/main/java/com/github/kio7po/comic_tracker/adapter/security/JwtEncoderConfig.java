package com.github.kio7po.comic_tracker.adapter.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class JwtEncoderConfig {

    @Bean
    public JwtEncoder jwtEncoder(@Value("${jwt.sign-key}") String signKey) {
        SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(signKey), "HmacSHA256");
        return NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
    }

}
