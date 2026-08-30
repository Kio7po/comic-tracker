package com.github.kio7po.comic_tracker.adapter.source.olympus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record OlympusMangaDto(String name, String slug, String type) {
}
