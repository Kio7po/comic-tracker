package com.github.kio7po.comic_tracker.domain.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.kio7po.comic_tracker.domain.common.OpaqueTokens;
import com.github.kio7po.comic_tracker.domain.entities.RefreshToken;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.enums.UserRole;
import com.github.kio7po.comic_tracker.domain.exceptions.EmailAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.InvalidCredentialsException;
import com.github.kio7po.comic_tracker.domain.exceptions.InvalidRefreshTokenException;
import com.github.kio7po.comic_tracker.domain.exceptions.UsernameAlreadyExistsException;
import com.github.kio7po.comic_tracker.domain.exceptions.WeakPasswordException;
import com.github.kio7po.comic_tracker.domain.port.persistence.RefreshTokenRepository;
import com.github.kio7po.comic_tracker.domain.port.persistence.UserRepository;
import com.github.kio7po.comic_tracker.domain.port.security.JwtIssuer;
import com.github.kio7po.comic_tracker.domain.port.security.PasswordHasher;

@Service
public class UserService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final JwtIssuer jwtIssuer;
    private final long refreshTokenExpirationDays;

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
            PasswordHasher passwordHasher, JwtIssuer jwtIssuer,
            @Value("${jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.jwtIssuer = jwtIssuer;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Transactional
    public User register(String username, String email, String rawPassword, String displayName) {
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new WeakPasswordException(MIN_PASSWORD_LENGTH);
        }
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException(username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHasher.hash(rawPassword));
        user.setDisplayName(displayName);
        user.setRole(UserRole.USER);

        return userRepository.save(user);
    }

    @Transactional
    public TokenPair login(String usernameOrEmail, String rawPassword) {
        User user = userRepository.findByUsername(usernameOrEmail)
                .or(() -> userRepository.findByEmail(usernameOrEmail))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokenPair(user);
    }

    /**
     * @param rawRefreshToken token from the client's cookie, or {@code null} if none was
     * presented. A {@code null} value is treated as an invalid token.
     */
    @Transactional
    public TokenPair refresh(@Nullable String rawRefreshToken) {
        if (rawRefreshToken == null) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(rawRefreshToken))
                .filter(token -> !token.isRevoked())
                .filter(token -> !token.isExpired())
                .orElseThrow(InvalidRefreshTokenException::new);

        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        return issueTokenPair(storedToken.getUser());
    }

    /**
     * @param rawRefreshToken token from the client's cookie, or {@code null} if none was
     * presented. A missing or unknown token is treated as a no-op to keep logout idempotent.
     */
    @Transactional
    public void logout(@Nullable String rawRefreshToken) {
        if (rawRefreshToken == null) {
            return;
        }

        refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    private TokenPair issueTokenPair(User user) {
        String accessToken = jwtIssuer.issue(user);
        String rawRefreshToken = OpaqueTokens.generate();
        Instant expiresAt = Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.issue(user, OpaqueTokens.hash(rawRefreshToken), expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, rawRefreshToken);
    }

}
