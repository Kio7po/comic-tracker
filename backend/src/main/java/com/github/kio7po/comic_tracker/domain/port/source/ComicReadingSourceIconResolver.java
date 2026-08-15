package com.github.kio7po.comic_tracker.domain.port.source;

import java.util.Optional;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;

public interface ComicReadingSourceIconResolver {
    boolean supports(ComicReadingSource source);
    Optional<String> resolveIconUrl(ComicReadingSource source);
}
