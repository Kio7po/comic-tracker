package com.github.kio7po.comic_tracker.adapter.metadata.tenrai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record TenraiNamedResourceDto(String name) {
}
