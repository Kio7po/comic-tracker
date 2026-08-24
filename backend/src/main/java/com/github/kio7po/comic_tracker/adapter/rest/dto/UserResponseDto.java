package com.github.kio7po.comic_tracker.adapter.rest.dto;

import com.github.kio7po.comic_tracker.domain.enums.UserRole;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        String displayName,
        String biography,
        String pictureUrl,
        String locale,
        UserRole role) {
}
