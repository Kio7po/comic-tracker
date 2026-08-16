package com.github.kio7po.comic_tracker.adapter.rest.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    public SecurityConfig(JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // matches rules top-to-bottom, first match wins.
                    .requestMatchers("/api/auth/me").authenticated()
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/catalog/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/comics/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reading-sources").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reading-entries").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/comics/*/reading-entries").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/reading-entries/*/approve",
                            "/api/reading-entries/*/reject").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/reading-sources/*/approve",
                            "/api/reading-sources/*/reject").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/v3/api-docs/**", "/scalar/**").permitAll()
                    .anyRequest().denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(
                    jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

}
