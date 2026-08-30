package com.github.kio7po.comic_tracker.domain.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TextSearchTest {

    @ParameterizedTest
    @CsvSource({
            "sword king, The Sword King, true",
            "king sword, The Sword King, true",
            "SwOrD, the sword king, true",
            "cafe, Café, true",
            "sword lord, The Sword King, false",
            "'', The Sword King, true",
            "'   ', The Sword King, true"
    })
    void matchesEveryWordAsASubstringAnywhereRegardlessOfOrderCaseOrAccents(String query, String alternative,
            boolean expected) {
        assertThat(TextSearch.matches(query, alternative)).isEqualTo(expected);
    }

    @Test
    void aWordCanMatchInADifferentAlternativeThanTheOthers() {
        assertThat(TextSearch.matches("king dragon", "The Sword King", "Dragon Chronicles")).isTrue();
    }

    @Test
    void falseWhenNoAlternativeCoversAllWords() {
        assertThat(TextSearch.matches("king lord", "The Sword King", "Dragon Chronicles")).isFalse();
    }

    @Test
    void treatsANullAlternativeAsAbsentRatherThanThrowing() {
        assertThat(TextSearch.matches("king", "The Sword King", null)).isTrue();
    }

}
