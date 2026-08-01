package com.github.kio7po.comic_tracker.adapter.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank @Size(max = 255) String usernameOrEmail,
        @NotBlank @Size(max = 255) String password) {
}
