package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One page of a source's search. The page size is set by the site's extension, it can't be requested.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SuwayomiSearchPageDto(Boolean hasNextPage, List<SuwayomiMangaDto> mangas) {
}
