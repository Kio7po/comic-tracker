package com.github.kio7po.comic_tracker.adapter.rest.dto;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record TokenResponseDto(String accessToken, String tokenType, long expiresIn) {
}
