package com.github.kio7po.comic_tracker.adapter.rest.dto;

import java.util.List;

public record PageResponseDto<T>(List<T> items, boolean existMoreItems, Integer totalItems) {
}
