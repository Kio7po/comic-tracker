package com.github.kio7po.comic_tracker.domain.service;

import java.time.Instant;

public record TokenPair(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
        Instant refreshTokenExpiresAt, boolean rememberMe) {
}
