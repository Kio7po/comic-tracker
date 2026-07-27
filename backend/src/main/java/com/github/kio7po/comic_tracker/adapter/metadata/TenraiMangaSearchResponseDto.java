package com.github.kio7po.comic_tracker.adapter.metadata;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record TenraiMangaSearchResponseDto(List<TenraiMangaDto> data, TenraiPaginationDto pagination) {
}
