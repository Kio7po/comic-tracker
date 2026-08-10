package com.github.kio7po.comic_tracker.domain.port.security;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {
}
