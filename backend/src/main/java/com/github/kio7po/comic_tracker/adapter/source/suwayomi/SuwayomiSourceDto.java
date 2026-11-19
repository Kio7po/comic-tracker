package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record SuwayomiSourceDto(String id, String name, String lang, String homeUrl) {
}
