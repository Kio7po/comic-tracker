package com.github.kio7po.comic_tracker.adapter.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record TenraiPaginationDto(boolean hasNextPage, TenraiPaginationItemsDto items) {
}
