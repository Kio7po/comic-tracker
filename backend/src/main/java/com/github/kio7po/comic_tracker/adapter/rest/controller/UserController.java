package com.github.kio7po.comic_tracker.adapter.rest.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.kio7po.comic_tracker.adapter.rest.dto.UserProfileRequestDto;
import com.github.kio7po.comic_tracker.adapter.rest.dto.UserResponseDto;
import com.github.kio7po.comic_tracker.adapter.rest.mapper.UserMapper;
import com.github.kio7po.comic_tracker.adapter.rest.security.CurrentUser;
import com.github.kio7po.comic_tracker.domain.entities.User;
import com.github.kio7po.comic_tracker.domain.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponseDto me(@CurrentUser Long userId) {
        return UserMapper.toResponseDto(userService.findById(userId));
    }

    @PutMapping("/me")
    public UserResponseDto updateProfile(@Valid @RequestBody UserProfileRequestDto request,
            @CurrentUser Long userId) {
        User user = userService.updateProfile(userId, request.displayName(), request.biography(),
                request.pictureUrl(), request.locale());
        return UserMapper.toResponseDto(user);
    }

}
