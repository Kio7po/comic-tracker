package com.github.kio7po.comic_tracker.adapter.rest.controller;

import java.time.Duration;
import java.time.Instant;

import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.dto.LoginRequestDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.RegisterRequestDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.TokenResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.UserResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.mapper.UserMapper;
import com.github.kio7po.comic_tracker.adapter.rest.security.CurrentUser;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.service.TokenPair;
import com.github.kio7po.comic_tracker.domain.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/auth";

    private final UserService userService;
    private final boolean refreshTokenSecure;

    public AuthController(UserService userService,
            @Value("${jwt.refresh-token-secure}") boolean refreshTokenSecure) {
        this.userService = userService;
        this.refreshTokenSecure = refreshTokenSecure;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
        User user = userService.register(request.username(), request.email(), request.password(),
                request.displayName());
        return UserMapper.toResponseDto(user);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        TokenPair tokenPair = userService.login(request.usernameOrEmail(), request.password(),
                request.rememberMe());
        return tokenResponse(tokenPair);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        TokenPair tokenPair = userService.refresh(refreshToken);
        return tokenResponse(tokenPair);
    }

    @GetMapping("/me")
    public UserResponseDto me(@CurrentUser Long userId) {
        return UserMapper.toResponseDto(userService.findById(userId));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        userService.logout(refreshToken);
        ResponseCookie clearCookie = refreshTokenCookie("", Duration.ZERO);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie.toString()).build();
    }

    private ResponseEntity<TokenResponseDto> tokenResponse(TokenPair tokenPair) {
        // Sin Max-Age (cookie de sesión, se borra al cerrar el navegador) si no se pidió.
        Duration maxAge = tokenPair.rememberMe()
                ? Duration.between(Instant.now(), tokenPair.refreshTokenExpiresAt())
                : null;
        ResponseCookie refreshCookie = refreshTokenCookie(tokenPair.refreshToken(), maxAge);
        long expiresInSeconds = Duration.between(Instant.now(), tokenPair.accessTokenExpiresAt()).getSeconds();
        TokenResponseDto body = new TokenResponseDto(tokenPair.accessToken(), "Bearer", expiresInSeconds);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).body(body);
    }

    private ResponseCookie refreshTokenCookie(String value, @Nullable Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(refreshTokenSecure)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_COOKIE_PATH);
        if (maxAge != null) {
            builder.maxAge(maxAge);
        }
        return builder.build();
    }

}
