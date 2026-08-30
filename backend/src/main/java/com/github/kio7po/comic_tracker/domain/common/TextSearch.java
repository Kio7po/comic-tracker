package com.github.kio7po.comic_tracker.domain.common;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public final class TextSearch {

    private TextSearch() {
    }

    /**
     * Case and accent-insensitive, multi-word substring match: every whitespace-separated word in
     * {@code query} must appear somewhere across {@code alternatives} (joined together), in any
     * order. A blank query matches everything.
     */
    public static boolean matches(String query, String... alternatives) {
        String normalizedQuery = normalize(query).trim();
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        String haystack = normalize(Arrays.stream(alternatives)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" ")));
        return Arrays.stream(normalizedQuery.split("\\s+")).allMatch(haystack::contains);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
