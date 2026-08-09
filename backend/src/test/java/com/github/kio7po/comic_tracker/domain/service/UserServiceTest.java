package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.github.kio7po.comic_tracker.domain.port.security.AccessToken;
import com.github.kio7po.comic_tracker.domain.port.security.JwtIssuer;
import com.github.kio7po.comic_tracker.domain.port.security.PasswordHasher;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String USERNAME = "testuser";
    private static final String EMAIL = "testuser@example.com";
    private static final String PASSWORD = "password123";
    private static final String DISPLAY_NAME = "Test User";
    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 30;
    private static final long REFRESH_TOKEN_SESSION_EXPIRATION_HOURS = 24;

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private JwtIssuer jwtIssuer;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, refreshTokenRepository, passwordHasher, jwtIssuer,
                REFRESH_TOKEN_EXPIRATION_DAYS, REFRESH_TOKEN_SESSION_EXPIRATION_HOURS);
    }

    private static User existingUser() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setEmail(EMAIL);
        user.setPasswordHash("hashed-password");
        user.setDisplayName(DISPLAY_NAME);
        user.setRole(UserRole.USER);
        return user;
    }

    private static AccessToken accessToken(String value) {
        return new AccessToken(value, Instant.now().plusSeconds(900));
    }

    private static RefreshToken validStoredToken(User user, boolean rememberMe) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setRememberMe(rememberMe);
        return token;
    }

    // ─── register ───────────────────────────────────────────────

    @Test
    void registerSavesUserWithHashedPasswordAndDefaultRole() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordHasher.hash(PASSWORD)).thenReturn("hashed-password");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.register(USERNAME, EMAIL, PASSWORD, DISPLAY_NAME);

        assertThat(result.getUsername()).isEqualTo(USERNAME);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(result.getDisplayName()).isEqualTo(DISPLAY_NAME);
        assertThat(result.getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void registerThrowsWeakPasswordExceptionWhenPasswordTooShort() {
        assertThatThrownBy(() -> userService.register(USERNAME, EMAIL, "short", DISPLAY_NAME))
                .isInstanceOf(WeakPasswordException.class);

        verifyNoInteractions(userRepository);
    }

    @Test
    void registerThrowsUsernameAlreadyExistsExceptionWhenUsernameTaken() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(existingUser()));

        assertThatThrownBy(() -> userService.register(USERNAME, EMAIL, PASSWORD, DISPLAY_NAME))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerThrowsEmailAlreadyExistsExceptionWhenEmailTaken() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(existingUser()));

        assertThatThrownBy(() -> userService.register(USERNAME, EMAIL, PASSWORD, DISPLAY_NAME))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    // ─── findById ───────────────────────────────────────────────

    @Test
    void findByIdReturnsUserWhenPresent() {
        User user = existingUser();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findById(1L);

        assertThat(result).isSameAs(user);
    }

    @Test
    void findByIdThrowsInvalidCredentialsExceptionWhenNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(1L)).isInstanceOf(InvalidCredentialsException.class);
    }

    // ─── login ──────────────────────────────────────────────────

    @Test
    void loginReturnsTokenPairWhenUsernameAndPasswordAreValid() {
        User user = existingUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
        when(jwtIssuer.issue(user)).thenReturn(accessToken("access-token"));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPair result = userService.login(USERNAME, PASSWORD, false);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(argThat(
                (RefreshToken token) -> token.getUser() == user && !token.isRevoked() && !token.isExpired()));
    }

    @Test
    void loginPropagatesRememberMeToTokenPairAndStoredRefreshToken() {
        User user = existingUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
        when(jwtIssuer.issue(user)).thenReturn(accessToken("access-token"));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPair result = userService.login(USERNAME, PASSWORD, true);

        assertThat(result.rememberMe()).isTrue();
        verify(refreshTokenRepository).save(argThat(RefreshToken::isRememberMe));
    }

    @Test
    void loginFallsBackToEmailLookupWhenUsernameNotFound() {
        User user = existingUser();
        when(userRepository.findByUsername(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.getPasswordHash())).thenReturn(true);
        when(jwtIssuer.issue(user)).thenReturn(accessToken("access-token"));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPair result = userService.login(EMAIL, PASSWORD, false);

        verify(userRepository).findByEmail(EMAIL);
        assertThat(result.accessToken()).isEqualTo("access-token");
    }

    @Test
    void loginThrowsInvalidCredentialsExceptionWhenUserNotFound() {
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(USERNAME, PASSWORD, false))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(passwordHasher, never()).matches(any(), any());
    }

    @Test
    void loginThrowsInvalidCredentialsExceptionWhenPasswordDoesNotMatch() {
        User user = existingUser();
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> userService.login(USERNAME, PASSWORD, false))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtIssuer, never()).issue(any());
    }

    // ─── refresh ────────────────────────────────────────────────

    @Test
    void refreshRotatesValidTokenAndIssuesNewPair() {
        User user = existingUser();
        RefreshToken storedToken = validStoredToken(user, false);
        String rawToken = "raw-refresh-token";
        when(refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(rawToken))).thenReturn(Optional.of(storedToken));
        when(jwtIssuer.issue(user)).thenReturn(accessToken("new-access-token"));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPair result = userService.refresh(rawToken);

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(storedToken.isRevoked()).isTrue();
        // once to persist the revoked old token, once for the newly issued one
        verify(refreshTokenRepository, times(2)).save(any());
    }

    @Test
    void refreshCarriesForwardRememberMeFromStoredToken() {
        User user = existingUser();
        RefreshToken storedToken = validStoredToken(user, true);
        String rawToken = "raw-refresh-token";
        when(refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(rawToken))).thenReturn(Optional.of(storedToken));
        when(jwtIssuer.issue(user)).thenReturn(accessToken("new-access-token"));
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        TokenPair result = userService.refresh(rawToken);

        assertThat(result.rememberMe()).isTrue();
        verify(refreshTokenRepository).save(argThat(
                (RefreshToken token) -> token != storedToken && token.isRememberMe()));
    }

    @Test
    void refreshThrowsInvalidRefreshTokenExceptionWhenTokenIsNull() {
        assertThatThrownBy(() -> userService.refresh(null)).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void refreshThrowsInvalidRefreshTokenExceptionWhenTokenIsUnknown() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.refresh("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshThrowsInvalidRefreshTokenExceptionWhenTokenIsRevoked() {
        RefreshToken revokedToken = validStoredToken(existingUser(), false);
        revokedToken.setRevokedAt(Instant.now());
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> userService.refresh("raw-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshThrowsInvalidRefreshTokenExceptionWhenTokenIsExpired() {
        RefreshToken expiredToken = validStoredToken(existingUser(), false);
        expiredToken.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> userService.refresh("raw-refresh-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    // ─── logout ─────────────────────────────────────────────────

    @Test
    void logoutDoesNothingWhenTokenIsNull() {
        userService.logout(null);

        verifyNoInteractions(refreshTokenRepository);
    }

    @Test
    void logoutDoesNothingWhenTokenIsUnknown() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        userService.logout("unknown");

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logoutRevokesKnownToken() {
        RefreshToken storedToken = validStoredToken(existingUser(), false);
        String rawToken = "raw-refresh-token";
        when(refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(rawToken))).thenReturn(Optional.of(storedToken));

        userService.logout(rawToken);

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
    }

}
