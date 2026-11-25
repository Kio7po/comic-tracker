package com.github.kio7po.comic_tracker.adapter.source.olympus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @param summary the work's synopsis; null in the listing, only the work's own page carries it.
 * @param status same as {@code summary}.
 * @param cover the public address of the work's cover.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OlympusMangaDto(Long id, String name, String slug, String type, String summary,
        OlympusMangaStatusDto status, String cover) {
}
