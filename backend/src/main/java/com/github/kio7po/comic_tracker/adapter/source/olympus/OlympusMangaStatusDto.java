package com.github.kio7po.comic_tracker.adapter.source.olympus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @param id Olympus' own identifier for the status; the stable one, since the name is a label that can be reworded.
 * @param name Olympus' own label for the status, in Spanish.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record OlympusMangaStatusDto(Integer id, String name) {
}
