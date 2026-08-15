package com.github.kio7po.comic_tracker.adapter.source;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.kio7po.comic_tracker.domain.entities.ComicReadingSource;

class FaviconComicReadingSourceIconResolverTest {

    private final FaviconComicReadingSourceIconResolver resolver = new FaviconComicReadingSourceIconResolver();

    @Test
    void supportsAnySource() {
        ComicReadingSource source = new ComicReadingSource();
        source.setUrl("https://mangadex.org");

        assertThat(resolver.supports(source)).isTrue();
    }

    @Test
    void resolvesToFaviconIcoUnderTheSourceUrl() {
        ComicReadingSource source = new ComicReadingSource();
        source.setUrl("https://mangadex.org");

        assertThat(resolver.resolveIconUrl(source)).contains("https://mangadex.org/favicon.ico");
    }

}
