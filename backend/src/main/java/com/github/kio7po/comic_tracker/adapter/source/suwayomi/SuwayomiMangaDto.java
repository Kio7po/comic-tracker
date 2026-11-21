package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @param id Suwayomi's own identifier for the work, not the site's.
 * @param realUrl the work's public address on the site; null when Suwayomi hasn't fetched the work's page yet, which
 *            is the case for almost every search result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SuwayomiMangaDto(Integer id, String title, String realUrl) {
}
