package com.github.kio7po.comic_tracker.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SlugifierTest {

    @ParameterizedTest
    @CsvSource({
            "Berserk, berserk",
            "One Piece, one-piece",
            "  Trim Me  , trim-me",
            "Jujutsu Kaisen: Nichijou, jujutsu-kaisen-nichijou",
            "Café con Días, cafe-con-dias"
    })
    void slugifiesTitles(String title, String expectedSlug) {
        assertThat(Slugifier.slugify(title)).isEqualTo(expectedSlug);
    }

    @ParameterizedTest
    @ValueSource(strings = { "!!!", "...", "   " })
    void throwsWhenTitleProducesAnEmptySlug(String title) {
        assertThatThrownBy(() -> Slugifier.slugify(title)).isInstanceOf(IllegalStateException.class);
    }

}
