package com.github.kio7po.comic_tracker.adapter.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
        @NotBlank @Size(max = 255) String username,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 255) String password,
        @NotBlank @Size(max = 255) String displayName) {
}
