package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @param uploadDate epoch milliseconds, sent as a string (GraphQL {@code LongString}); "0" when the site gives none.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SuwayomiChapterDto(String uploadDate) {
}
