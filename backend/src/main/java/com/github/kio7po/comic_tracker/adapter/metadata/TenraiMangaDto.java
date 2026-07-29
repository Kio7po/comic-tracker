package com.github.kio7po.comic_tracker.adapter.metadata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record TenraiMangaDto(
        long malId,
        String title,
        String titleEnglish,
        String titleJapanese,
        List<String> titleSynonyms,
        String synopsis,
        TenraiImagesDto images,
        String type,
        String status,
        TenraiPublishedDto published,
        Integer chapters,
        List<TenraiNamedResourceDto> authors,
        List<TenraiNamedResourceDto> genres,
        List<TenraiNamedResourceDto> explicitGenres,
        List<TenraiNamedResourceDto> themes,
        List<TenraiNamedResourceDto> demographics) {
}
