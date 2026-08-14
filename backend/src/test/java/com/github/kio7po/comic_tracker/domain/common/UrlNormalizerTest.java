package com.github.kio7po.comic_tracker.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UrlNormalizerTest {

    @ParameterizedTest
    @CsvSource({
            "HTTPS://MangaDex.org, https://mangadex.org",
            "https://mangadex.org/, https://mangadex.org",
            "https://mangadex.org/title/123/, https://mangadex.org/title/123",
            "https://mangadex.org/title/123, https://mangadex.org/title/123",
            "https://mangadex.org/title/123?chapter=5, https://mangadex.org/title/123?chapter=5",
            "https://mangadex.org/title/123#notes, https://mangadex.org/title/123#notes"
    })
    void normalizesCaseAndTrailingSlashOnly(String url, String expected) {
        assertThat(UrlNormalizer.normalize(url)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "HTTPS://MangaDex.org, https://mangadex.org",
            "https://mangadex.org, https://mangadex.org",
            "https://mangadex.org/title/123/chapter-5?lang=es, https://mangadex.org",
            "https://mangadex.org/title/456/chapter-1, https://mangadex.org"
    })
    void normalizeOriginDropsPathQueryAndFragment(String url, String expected) {
        assertThat(UrlNormalizer.normalizeOrigin(url)).isEqualTo(expected);
    }

}
