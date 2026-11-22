package com.github.kio7po.comic_tracker.adapter.source.suwayomi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @param id Suwayomi's own identifier for the work, not the site's.
 * @param realUrl the work's public address on the site; null when Suwayomi hasn't fetched the work's page yet, which
 *            is the case for almost every search result.
 * @param author null in search results and lookups: it is only asked for, and filled in by Suwayomi, when the work's
 *            page is fetched.
 * @param description same as {@code author}.
 * @param status one of Suwayomi's {@code MangaStatus} names, null when not asked for; "UNKNOWN" when asked for but
 *            the site doesn't say.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SuwayomiMangaDto(Integer id, String title, String realUrl, String author, String description, String status) {
}
