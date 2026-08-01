package com.github.kio7po.comic_tracker.domain.port.persistence;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.RefreshToken;

public interface RefreshTokenRepository {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    RefreshToken save(RefreshToken refreshToken);
}
