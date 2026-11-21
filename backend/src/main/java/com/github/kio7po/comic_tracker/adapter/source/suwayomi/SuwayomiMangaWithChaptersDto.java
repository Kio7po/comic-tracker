package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record SuwayomiMangaWithChaptersDto(SuwayomiMangaDto manga, List<SuwayomiChapterDto> chapters) {
}
