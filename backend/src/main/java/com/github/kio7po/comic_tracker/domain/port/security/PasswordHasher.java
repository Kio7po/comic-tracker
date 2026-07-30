package com.github.kio7po.comic_tracker.domain.port.security;

public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hash);
}
