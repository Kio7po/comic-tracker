package com.github.kio7po.comic_tracker.adapter.source;

import java.util.Optional;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingSourceIconResolver;

/**
 * Assumes the well-known /favicon.ico convention. Always matches, so it stays
 * lowest-priority. Any source-specific resolver added later should run before it.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class FaviconComicReadingSourceIconResolver implements ComicReadingSourceIconResolver {

    @Override
    public boolean supports(ComicReadingSource source) {
        return true;
    }

    @Override
    public Optional<String> resolveIconUrl(ComicReadingSource source) {
        return Optional.of(source.getUrl() + "/favicon.ico");
    }

}
