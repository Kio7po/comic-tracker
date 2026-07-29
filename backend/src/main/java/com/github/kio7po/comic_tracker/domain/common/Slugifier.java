package com.github.kio7po.comic_tracker.domain.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class Slugifier {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("^-+|-+$");

    private Slugifier() {
    }

    public static String slugify(String value) {
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(value, Normalizer.Form.NFKD))
                .replaceAll("");

        String slug = EDGE_DASHES
                .matcher(NON_ALPHANUMERIC.matcher(withoutDiacritics.toLowerCase(Locale.ROOT)).replaceAll("-"))
                .replaceAll("");

        if (slug.isEmpty()) {
            throw new IllegalStateException("Title '" + value + "' produces an empty slug");
        }
        return slug;
    }

}
