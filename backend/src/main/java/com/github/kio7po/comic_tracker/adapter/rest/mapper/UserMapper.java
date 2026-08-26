package com.github.kio7po.comic_tracker.adapter.rest.mapper;

import com.github.kio7po.comic_tracker.adapter.rest.dto.UserResponseDto;
import com.github.kio7po.comic_tracker.domain.entities.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponseDto toResponseDto(User user) {
        return new UserResponseDto(user.getId(), user.getUsername(), user.getEmail(), user.getDisplayName(),
                user.getBiography(), user.getPictureUrl(), user.getLocale(), user.getRole());
    }

}
