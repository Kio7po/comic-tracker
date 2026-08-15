package com.github.kio7po.comic_tracker.adapter.rest.dto.validation;

import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidLocaleValidator implements ConstraintValidator<ValidLocale, String> {

    private static final Set<String> ISO_639_1_LANGUAGES = Set.of(Locale.getISOLanguages());
    private static final Set<String> ISO_3166_ALPHA_2_COUNTRIES = Set.of(Locale.getISOCountries());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String[] parts = value.split("-", 2);
        if (!ISO_639_1_LANGUAGES.contains(parts[0].toLowerCase(Locale.ROOT))) {
            return false;
        }

        return parts.length == 1 || ISO_3166_ALPHA_2_COUNTRIES.contains(parts[1].toUpperCase(Locale.ROOT));
    }

}
