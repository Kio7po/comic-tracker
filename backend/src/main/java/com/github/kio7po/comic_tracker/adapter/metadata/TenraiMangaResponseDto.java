package com.github.kio7po.comic_tracker.adapter.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record TenraiMangaResponseDto(TenraiMangaDto data) {
}
