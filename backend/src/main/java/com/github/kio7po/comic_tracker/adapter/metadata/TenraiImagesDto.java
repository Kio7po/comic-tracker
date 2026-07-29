package com.github.kio7po.comic_tracker.adapter.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
record TenraiImagesDto(TenraiImageSetDto jpg, TenraiImageSetDto webp) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record TenraiImageSetDto(String imageUrl, String smallImageUrl, String largeImageUrl) {
    }
}
