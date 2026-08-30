package com.github.kio7po.comic_tracker.adapter.source.olympus;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record OlympusChapterListResponseDto(List<OlympusChapterDto> data, OlympusChapterMetaDto meta) {
}
