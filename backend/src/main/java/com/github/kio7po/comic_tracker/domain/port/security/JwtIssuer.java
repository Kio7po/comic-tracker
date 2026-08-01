package com.github.kio7po.comic_tracker.domain.port.security;

import com.github.kio7po.comic_tracker.domain.entities.User;

public interface JwtIssuer {
    String issue(User user);
}
