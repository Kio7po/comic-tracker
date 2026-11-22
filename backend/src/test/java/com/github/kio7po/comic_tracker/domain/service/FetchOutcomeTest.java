package com.github.kio7po.comic_tracker.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.github.kio7po.comic_tracker.domain.exceptions.ComicNotFoundInReadingSourceException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingProviderFailedException;
import com.github.kio7po.comic_tracker.domain.exceptions.ComicReadingSourceNotSupportedException;
import com.github.kio7po.comic_tracker.domain.port.source.ComicReadingEntryDetails;

class FetchOutcomeTest {

    private static final Long SOURCE_ID = 1L;
    private static final String PROVIDER_KEY = "suwayomi";
    private static final String EXTERNAL_ID = "42";

    @Test
    void foundOrThrowReturnsTheFetchedEntryWhenFound() {
        FetchedEntry fetched = new FetchedEntry(PROVIDER_KEY,
                new ComicReadingEntryDetails(EXTERNAL_ID, "https://example.com/berserk", "Berserk", 374, null,
                        null, null, null));

        assertThat(new FetchOutcome.Found(fetched).foundOrThrow(SOURCE_ID, PROVIDER_KEY, EXTERNAL_ID))
                .isSameAs(fetched);
    }

    @Test
    void foundOrThrowThrowsWhenTheWorkDoesNotExist() {
        FetchOutcome outcome = new FetchOutcome.NotFound();

        assertThatThrownBy(() -> outcome.foundOrThrow(SOURCE_ID, PROVIDER_KEY, EXTERNAL_ID))
                .isInstanceOfSatisfying(ComicNotFoundInReadingSourceException.class, exception -> {
                    assertThat(exception.getSourceId()).isEqualTo(SOURCE_ID);
                    assertThat(exception.getExternalId()).isEqualTo(EXTERNAL_ID);
                });
    }

    @Test
    void foundOrThrowThrowsKeepingTheCauseWhenTheProviderFailed() {
        RuntimeException cause = new RuntimeException("site is down");
        FetchOutcome outcome = new FetchOutcome.Failed(cause);

        assertThatThrownBy(() -> outcome.foundOrThrow(SOURCE_ID, PROVIDER_KEY, EXTERNAL_ID))
                .isInstanceOfSatisfying(ComicReadingProviderFailedException.class, exception -> {
                    assertThat(exception.getSourceId()).isEqualTo(SOURCE_ID);
                    assertThat(exception.getProviderKey()).isEqualTo(PROVIDER_KEY);
                    assertThat(exception.getCause()).isSameAs(cause);
                });
    }

    @Test
    void foundOrThrowThrowsWhenNoProviderServesTheSource() {
        FetchOutcome outcome = new FetchOutcome.NotSupported();

        assertThatThrownBy(() -> outcome.foundOrThrow(SOURCE_ID, PROVIDER_KEY, EXTERNAL_ID))
                .isInstanceOfSatisfying(ComicReadingSourceNotSupportedException.class,
                        exception -> assertThat(exception.getSourceId()).isEqualTo(SOURCE_ID));
    }

}
