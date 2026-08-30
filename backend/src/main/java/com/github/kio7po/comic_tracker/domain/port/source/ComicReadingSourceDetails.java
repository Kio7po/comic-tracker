package com.github.kio7po.comic_tracker.domain.port.source;

import java.time.Instant;

public record ComicReadingSourceDetails(String title, Integer availableChapters, Instant latestChapterAt) {
}
