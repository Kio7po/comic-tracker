package com.github.kio7po.comic_tracker.adapter.rest.dto.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidLocaleValidatorTest {

    private final ValidLocaleValidator validator = new ValidLocaleValidator();

    @ParameterizedTest
    @ValueSource(strings = { "es", "en", "ES", "es-ES", "en-US", "pt-BR" })
    void acceptsValidLanguageAndLanguageRegionTags(String locale) {
        assertThat(validator.isValid(locale, null)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "xx", // not a real ISO 639-1 language
            "es-XX", // real language, not a real ISO 3166-1 region
            "es-", // trailing dash, no region
            "not-a-locale"
    })
    void rejectsInvalidTags(String locale) {
        assertThat(validator.isValid(locale, null)).isFalse();
    }

}
