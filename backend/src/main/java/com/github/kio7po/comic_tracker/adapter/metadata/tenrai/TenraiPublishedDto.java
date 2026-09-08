package com.github.kio7po.comic_tracker.adapter.metadata.tenrai;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record TenraiPublishedDto(OffsetDateTime from, OffsetDateTime to) {
}
